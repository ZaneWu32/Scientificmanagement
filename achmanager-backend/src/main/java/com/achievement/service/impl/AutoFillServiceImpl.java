package com.achievement.service.impl;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.achievement.client.LlmClient;
import com.achievement.constant.LlmUsage;
import com.achievement.domain.ChatMessage;
import com.achievement.domain.vo.AutoFillResultVO;
import com.achievement.domain.vo.AutoFillResultVO.AutoFillFieldVO;
import com.achievement.service.IAutoFillService;
import com.achievement.service.ITextExtractionService;
import com.achievement.service.ITextExtractionService.ExtractionResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoFillServiceImpl implements IAutoFillService {

    private final ITextExtractionService textExtractionService;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    private static final double CONFIDENCE_THRESHOLD = 0.75;

    @Override
    public AutoFillResultVO recognizeFields(MultipartFile file, String resultTypeCode) {
        String fileName = file.getOriginalFilename();

        // 1. 文本提取
        ExtractionResult extraction;
        try {
            extraction = textExtractionService.extractText(
                    file.getBytes(), fileName, file.getContentType());
        } catch (Exception e) {
            throw new RuntimeException("文件读取失败: " + e.getMessage(), e);
        }

        String text = extraction.text();
        if (text == null || text.isBlank()) {
            throw new RuntimeException("无法从文件中提取文本，请确认文件格式正确且非扫描件");
        }

        // 2. 截取（论文只取前 N 字符，因为元数据集中在首页）
        String truncated = truncateForType(text, resultTypeCode);
        boolean wasTruncated = truncated.length() < text.length();

        // 3. 调用 LLM 识别字段
        String rawResponse = callLlm(truncated, resultTypeCode);

        // 4. 解析响应
        AutoFillResultVO result = parseLlmResponse(rawResponse, fileName);

        // 5. 用 Tika 提取的元数据补充/覆盖高置信度字段
        applyTikaMetadata(result, extraction);

        // 6. 标记待确认字段
        markNeedsConfirm(result);

        return result;
    }

    // ── 文本截取 ──

    private String truncateForType(String text, String resultTypeCode) {
        int limit = switch (normalizeTypeCode(resultTypeCode)) {
            case "paper", "journal" -> 6000;
            case "software" -> 20000;  // 软著全文通常较短，给更多空间
            default -> 8000;
        };
        if (limit <= 0 || text.length() <= limit) {
            return text;
        }
        return text.substring(0, limit);
    }

    private String normalizeTypeCode(String resultTypeCode) {
        if (resultTypeCode == null) {
            return "default";
        }
        return switch (resultTypeCode.toLowerCase()) {
            case "paper", "journal", "thesis", "期刊", "论文" -> "paper";
            case "software", "软著", "软件著作权" -> "software";
            default -> resultTypeCode.toLowerCase();
        };
    }

    // ── LLM 调用 ──

    private String callLlm(String text, String resultTypeCode) {
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(text, resultTypeCode);

        String responseJson = llmClient.chatCompletion(
                LlmUsage.DEFAULT,
                List.of(ChatMessage.system(systemPrompt), ChatMessage.user(userPrompt)),
                0.1,
                2048);

        // 从 OpenAI 格式的响应中提取 content
        try {
            return objectMapper.readTree(responseJson)
                    .at("/choices/0/message/content").asText("");
        } catch (Exception e) {
            log.warn("解析 LLM 响应失败: {}", e.getMessage());
            throw new RuntimeException("AI 服务返回格式异常");
        }
    }

    private String buildSystemPrompt() {
        return """
                你是科研成果管理场景中的文档元数据识别专家。请从提供的文档文本中识别成果的结构化元数据字段。

                要求：
                1. 只根据文档文本内容识别，不要编造不存在的信息。
                2. 如果某个字段在文本中无法识别或不确定，将 value 设为空字符串，confidence 设为 0。
                3. sourceSnippet 必须是从文档原文中摘取的、作为识别依据的原始片段，不超过 200 字。
                4. confidence 表示你对识别结果的置信度，范围 0-1。

                只返回 JSON，不要返回 Markdown、解释性段落或代码块。格式如下：
                {
                  "fields": [
                    {
                      "key": "字段标识符",
                      "label": "字段中文名称",
                      "value": "识别出的值",
                      "confidence": 0.95,
                      "sourceSnippet": "原文依据片段"
                    }
                  ]
                }
                """;
    }

    private String buildUserPrompt(String text, String resultTypeCode) {
        String fieldSpec = switch (normalizeTypeCode(resultTypeCode)) {
            case "paper" -> """
                    请识别以下字段：
                    - title: 成果标题
                    - authors: 作者（多个用分号分隔）
                    - year: 发表/出版年份（4位数字）
                    - abstract: 摘要
                    - keywords: 关键词（多个用分号分隔）
                    - doi: DOI 编号
                    - journalName: 期刊或会议名称
                    - institution: 作者所属单位/机构
                    - fundingSource: 资助来源/基金项目
                    """;
            case "software" -> """
                    请识别以下字段：
                    - title: 软件名称
                    - authors: 著作权人（多个用分号分隔）
                    - year: 首次发表年份（4位数字）
                    - registrationNumber: 软件登记号
                    - softwareType: 软件类型（如：应用软件、系统软件等）
                    - abstract: 功能简介
                    """;
            default -> """
                    请识别以下字段：
                    - title: 成果标题
                    - authors: 作者（多个用分号分隔）
                    - year: 年份（4位数字）
                    - abstract: 摘要
                    - keywords: 关键词（多个用分号分隔）
                    """;
        };

        return fieldSpec + "\n\n以下是文档文本内容：\n\n" + text;
    }

    // ── 响应解析 ──

    private AutoFillResultVO parseLlmResponse(String rawContent, String fileName) {
        AutoFillResultVO result = new AutoFillResultVO();
        result.setFileName(fileName);
        result.setRecognizedAt(OffsetDateTime.now().toString());
        result.setFields(new ArrayList<>());
        result.setPendingConfirmations(new ArrayList<>());

        String clean = stripCodeBlockMarkers(rawContent);
        JsonNode root;
        try {
            root = objectMapper.readTree(clean);
        } catch (Exception e) {
            log.warn("LLM 返回的 JSON 解析失败: {}", e.getMessage());
            return result;
        }

        JsonNode fieldsNode = root.isArray() ? root : root.path("fields");
        if (!fieldsNode.isArray()) {
            log.warn("LLM 返回中未找到 fields 数组");
            return result;
        }

        for (JsonNode node : fieldsNode) {
            AutoFillFieldVO field = new AutoFillFieldVO();
            field.setKey(node.path("key").asText(""));
            field.setLabel(node.path("label").asText(""));
            field.setValue(node.path("value").asText(""));
            field.setConfidence(clamp(node.path("confidence").asDouble(0D)));
            field.setSourceSnippet(node.path("sourceSnippet").asText(null));
            field.setNeedsConfirm(false);  // 后续由 markNeedsConfirm 处理
            result.getFields().add(field);
        }

        // 解析 pendingConfirmations（如果 LLM 返回了）
        JsonNode pendingNode = root.path("pendingConfirmations");
        if (pendingNode.isArray()) {
            for (JsonNode node : pendingNode) {
                String text = node.asText(null);
                if (text != null && !text.isBlank()) {
                    result.getPendingConfirmations().add(text);
                }
            }
        }

        return result;
    }

    // ── Tika 元数据补充 ──

    private void applyTikaMetadata(AutoFillResultVO result, ExtractionResult extraction) {
        Map<String, AutoFillFieldVO> fieldMap = new LinkedHashMap<>();
        for (AutoFillFieldVO field : result.getFields()) {
            fieldMap.put(field.getKey(), field);
        }

        // Tika 提取的 title 通常比 LLM 更准确（来自 PDF 元数据）
        if (extraction.title() != null && !extraction.title().isBlank()) {
            AutoFillFieldVO existing = fieldMap.get("title");
            if (existing == null || existing.getValue().isBlank() || existing.getConfidence() < 0.9) {
                AutoFillFieldVO tikaField = new AutoFillFieldVO();
                tikaField.setKey("title");
                tikaField.setLabel("成果标题");
                tikaField.setValue(extraction.title());
                tikaField.setConfidence(0.98);
                tikaField.setSourceSnippet("[来自文档元数据]");
                tikaField.setNeedsConfirm(false);
                replaceOrAddField(result.getFields(), tikaField);
            }
        }

        // Tika 提取的 author
        if (extraction.author() != null && !extraction.author().isBlank()) {
            AutoFillFieldVO existing = fieldMap.get("authors");
            if (existing == null || existing.getValue().isBlank() || existing.getConfidence() < 0.9) {
                AutoFillFieldVO tikaField = new AutoFillFieldVO();
                tikaField.setKey("authors");
                tikaField.setLabel("作者");
                tikaField.setValue(extraction.author());
                tikaField.setConfidence(0.96);
                tikaField.setSourceSnippet("[来自文档元数据]");
                tikaField.setNeedsConfirm(false);
                replaceOrAddField(result.getFields(), tikaField);
            }
        }
    }

    private void replaceOrAddField(List<AutoFillFieldVO> fields, AutoFillFieldVO newField) {
        for (int i = 0; i < fields.size(); i++) {
            if (fields.get(i).getKey().equals(newField.getKey())) {
                fields.set(i, newField);
                return;
            }
        }
        fields.add(0, newField);  // 新字段插入到列表开头
    }

    // ── 后处理 ──

    private void markNeedsConfirm(AutoFillResultVO result) {
        for (AutoFillFieldVO field : result.getFields()) {
            if (field.getValue().isBlank()) {
                field.setNeedsConfirm(true);
                field.setConfidence(0);
            } else if (field.getConfidence() < CONFIDENCE_THRESHOLD) {
                field.setNeedsConfirm(true);
            }
        }
    }

    // ── 工具方法 ──

    private String stripCodeBlockMarkers(String content) {
        if (content == null) {
            return "{}";
        }
        String text = content.trim();
        if (text.startsWith("```")) {
            int firstLineEnd = text.indexOf('\n');
            if (firstLineEnd >= 0) {
                text = text.substring(firstLineEnd + 1).trim();
            }
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3).trim();
        }
        return text;
    }

    private double clamp(double score) {
        if (score < 0D) {
            return 0D;
        }
        if (score > 1D) {
            return 1D;
        }
        return score;
    }
}
