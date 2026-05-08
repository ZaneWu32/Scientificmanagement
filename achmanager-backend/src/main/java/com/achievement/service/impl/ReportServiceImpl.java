package com.achievement.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.achievement.client.LlmClient;
import com.achievement.constant.LlmUsage;
import com.achievement.domain.ChatMessage;
import com.achievement.domain.vo.AchDetailVO;
import com.achievement.domain.vo.AchFieldVO;
import com.achievement.domain.vo.ReportTaskVO;
import com.achievement.service.IAchievementMainsService;
import com.achievement.service.IReportService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements IReportService {

    private final IAchievementMainsService achievementMainsService;
    private final LlmClient llmClient;
    private final ReportRenderService reportRenderService;
    private final ObjectMapper objectMapper;
    @Qualifier("reportTaskExecutor")
    private final Executor reportTaskExecutor;

    private final ConcurrentHashMap<String, ReportTask> taskMap = new ConcurrentHashMap<>();

    @Override
    public String startGeneration(List<String> achievementDocIds) {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        ReportTask task = new ReportTask();
        task.setId(taskId);
        task.setStatus("PENDING");
        task.setProgress(0);
        task.setAchievementDocIds(achievementDocIds);
        task.setCreatedAt(LocalDateTime.now());
        taskMap.put(taskId, task);
        reportTaskExecutor.execute(() -> doGenerate(task));
        cleanExpiredTasks();
        return taskId;
    }

    public void doGenerate(ReportTask task) {
        task.setStatus("GENERATING");
        task.setProgress(10);

        try {
            List<AchDetailVO> details = new ArrayList<>();
            List<String> docIds = task.getAchievementDocIds();
            for (int i = 0; i < docIds.size(); i++) {
                AchDetailVO detail = achievementMainsService.selectDetailForProjectSystem(docIds.get(i));
                if (detail != null) {
                    details.add(detail);
                }
                task.setProgress(10 + (int) ((double) (i + 1) / docIds.size() * 40));
            }

            if (details.isEmpty()) {
                task.setStatus("FAILED");
                task.setErrorMsg("未找到有效的成果物数据");
                return;
            }

            task.setProgress(50);

            String userPrompt = buildUserPrompt(details);
            String systemPrompt = buildSystemPrompt();

            task.setProgress(60);

            String responseJson = llmClient.chatCompletion(LlmUsage.REPORT, List.of(
                    ChatMessage.system(systemPrompt),
                    ChatMessage.user(userPrompt)));
            String htmlContent = objectMapper.readTree(responseJson).at("/choices/0/message/content").asText();

            // 如果LLM返回的内容包含markdown代码块标记，去除之
            htmlContent = stripCodeBlockMarkers(htmlContent);

            task.setReportHtml(htmlContent);
            task.setStatus("COMPLETED");
            task.setProgress(100);
            task.setCompletedAt(LocalDateTime.now());

        } catch (Exception e) {
            log.error("Report generation failed for task {}", task.getId(), e);
            task.setStatus("FAILED");
            task.setErrorMsg(e.getMessage());
        }
    }

    @Override
    public ReportTaskVO getTaskStatus(String taskId) {
        ReportTask task = taskMap.get(taskId);
        if (task == null)
            return null;

        ReportTaskVO vo = new ReportTaskVO();
        vo.setTaskId(task.getId());
        vo.setStatus(task.getStatus());
        vo.setProgress(task.getProgress());
        vo.setErrorMsg(task.getErrorMsg());
        return vo;
    }

    @Override
    public String getTaskContent(String taskId) {
        ReportTask task = taskMap.get(taskId);
        if (task == null || !"COMPLETED".equals(task.getStatus()))
            return null;
        return task.getReportHtml();
    }

    @Override
    public byte[] exportReport(String taskId, String format, String editedHtml) {
        ReportTask task = taskMap.get(taskId);
        if (task == null)
            throw new RuntimeException("任务不存在");

        String html = (editedHtml != null && !editedHtml.isBlank()) ? editedHtml : task.getReportHtml();
        if (html == null || html.isBlank())
            throw new RuntimeException("报告内容为空");

        if (!"word".equalsIgnoreCase(format)) {
            throw new RuntimeException("不支持的导出格式: " + format);
        }
        return reportRenderService.renderWord(html);
    }

    private String buildSystemPrompt() {
        return """
                你是一位专业的科研报告撰写助手。请根据提供的多个科研成果物信息，生成一份完整的综合研究报告。

                报告必须以HTML格式返回，结构如下：

                1. **封面**：包含报告标题"科研成果综合分析报告"、生成日期、成果物数量概要
                2. **目录**：列出各章节标题
                3. **概述**：对所有成果物进行总体概括
                4. **各成果物详细分析**：每个成果物一个章节，包含：
                    - 研究内容摘要
                    - 主要创新点
                    - 应用前景分析
                    - 关键词与研究方向
                5. **综合研究动态**：分析这些成果物所涉及领域的整体研究趋势和发展方向
                6. **结论与建议**：总结性评价和未来研究建议

                要求：
                - 直接返回HTML内容，不要包含```html等代码块标记
                - 使用语义化HTML标签（h1, h2, h3, p, ul, li, table等）
                - 内容要专业、客观、有深度
                - 使用中文撰写
                - HTML需要是完整的，可以直接渲染
                """;
    }

    private String buildUserPrompt(List<AchDetailVO> details) {
        StringBuilder sb = new StringBuilder();
        sb.append("以下是需要分析的科研成果物信息：\n\n");

        for (int i = 0; i < details.size(); i++) {
            AchDetailVO d = details.get(i);
            sb.append("### 成果物 ").append(i + 1).append("\n");
            sb.append("- 标题: ").append(d.getTitle()).append("\n");
            sb.append("- 类型: ").append(d.getTypeName()).append("\n");
            sb.append("- 年份: ").append(d.getYear()).append("\n");
            if (d.getAuthors() != null && !d.getAuthors().isEmpty()) {
                sb.append("- 作者: ").append(String.join(", ", d.getAuthors())).append("\n");
            }
            if (d.getKeywords() != null && !d.getKeywords().isEmpty()) {
                sb.append("- 关键词: ").append(String.join(", ", d.getKeywords())).append("\n");
            }
            if (d.getSummary() != null && !d.getSummary().isBlank()) {
                sb.append("- 摘要: ").append(d.getSummary()).append("\n");
            }
            if (d.getProjectName() != null) {
                sb.append("- 所属项目: ").append(d.getProjectName()).append("\n");
            }
            if (d.getFields() != null && !d.getFields().isEmpty()) {
                sb.append("- 详细字段:\n");
                for (AchFieldVO field : d.getFields()) {
                    if (field.getValue() != null) {
                        sb.append("  - ").append(field.getFieldName()).append(": ").append(field.getValue())
                                .append("\n");
                    }
                }
            }
            sb.append("\n");
        }

        sb.append("请基于以上").append(details.size()).append("个成果物的信息，生成一份完整的综合研究报告。");
        return sb.toString();
    }

    private String stripCodeBlockMarkers(String content) {
        if (content == null)
            return null;
        content = content.trim();
        if (content.startsWith("```html")) {
            content = content.substring(7);
        } else if (content.startsWith("```")) {
            content = content.substring(3);
        }
        if (content.endsWith("```")) {
            content = content.substring(0, content.length() - 3);
        }
        return content.trim();
    }

    private void cleanExpiredTasks() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);
        taskMap.entrySet().removeIf(entry -> entry.getValue().getCreatedAt().isBefore(threshold));
    }

    @Data
    public static class ReportTask {
        private String id;
        private String status;
        private int progress;
        private List<String> achievementDocIds;
        private String reportHtml;
        private String errorMsg;
        private LocalDateTime createdAt;
        private LocalDateTime completedAt;
    }
}
