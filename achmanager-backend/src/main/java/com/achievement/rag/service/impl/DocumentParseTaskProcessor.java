package com.achievement.rag.service.impl;

import com.achievement.config.ProcessSystemProperties;
import com.achievement.domain.vo.AchTypeDef;
import com.achievement.domain.vo.AchTypeDetailVO;
import com.achievement.rag.extract.LlmFieldExtractor;
import com.achievement.rag.model.ExtractedFieldValue;
import com.achievement.rag.model.ParseTask;
import com.achievement.rag.parser.DocumentParser;
import com.achievement.rag.parser.ParsedDocument;
import com.achievement.rag.parser.TextCleaner;
import com.achievement.rag.service.IDocumentParsingService;
import com.achievement.service.IAchievementTypesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 文档解析异步处理器
 * <p>
 * 独立 @Component 以确保 @Async 代理生效
 * </p>
 */
@Slf4j
@Component
public class DocumentParseTaskProcessor {

    private final ProcessSystemProperties processSystemProperties;
    private final DocumentParser documentParser;
    private final TextCleaner textCleaner;
    private final LlmFieldExtractor llmFieldExtractor;
    private final IAchievementTypesService achievementTypesService;
    private final IDocumentParsingService documentParsingService;

    public DocumentParseTaskProcessor(ProcessSystemProperties processSystemProperties,
                                      DocumentParser documentParser,
                                      TextCleaner textCleaner,
                                      LlmFieldExtractor llmFieldExtractor,
                                      IAchievementTypesService achievementTypesService,
                                      IDocumentParsingService documentParsingService) {
        this.processSystemProperties = processSystemProperties;
        this.documentParser = documentParser;
        this.textCleaner = textCleaner;
        this.llmFieldExtractor = llmFieldExtractor;
        this.achievementTypesService = achievementTypesService;
        this.documentParsingService = documentParsingService;
    }

    @Async("reportTaskExecutor")
    public void processTask(String taskId) {
        ParseTask task = documentParsingService.getTask(taskId);
        if (task == null) {
            log.warn("[{}] Task not found", taskId);
            return;
        }

        long startTime = System.currentTimeMillis();

        try {
            // ---- 阶段 1: 解析文档 ----
            task.setParseStatus("parsing");
            log.info("[{}] 开始解析文档...", taskId);

            Path basePath = Paths.get(processSystemProperties.getFileStorage().getBasePath());
            Path fullPath = basePath.resolve(task.getFilePath());

            ParsedDocument parsed = documentParser.parse(fullPath, task.getMimeType(), task.getFileName());

            // ---- 阶段 2: 文本清洗 ----
            task.setParseStatus("cleaning");
            log.info("[{}] 开始清洗文本...", taskId);

            String rawText = parsed.getRawText();
            String cleanedText = textCleaner.clean(rawText);
            task.setCleanedText(cleanedText);

            // ---- 阶段 3: 获取字段定义（如果尚未获取或有变化） ----
            if (task.getFieldDefinitions() == null || task.getFieldDefinitions().isEmpty()) {
                try {
                    AchTypeDetailVO typeDetail = achievementTypesService.selectDetail(task.getTypeDocId());
                    task.setTypeCode(typeDetail.getTypeCode());
                    task.setTypeName(typeDetail.getTypeName());
                    task.setFieldDefinitions(typeDetail.getFieldDefinitions());
                } catch (Exception e) {
                    log.warn("[{}] 提取阶段获取类型定义失败: {}", taskId, e.getMessage());
                    task.setFieldDefinitions(Collections.emptyList());
                }
            }

            // ---- 阶段 4: 字段提取（优先 LLM，自动降级正则） ----
            task.setParseStatus("extracting");
            List<AchTypeDef> defs = task.getFieldDefinitions();
            log.info("[{}] 开始字段提取, 字段定义数={}", taskId, defs != null ? defs.size() : 0);

            if (defs != null && !defs.isEmpty()) {
                List<ExtractedFieldValue> extracted = llmFieldExtractor.extract(cleanedText, defs);
                task.setExtractedFields(extracted);
            }

            long elapsed = System.currentTimeMillis() - startTime;
            task.setParseStatus("completed");
            task.setUpdateTime(LocalDateTime.now());

            log.info("[{}] 解析完成: 耗时={}ms, 清洗文本长度={}, 提取字段数={}",
                    taskId, elapsed, cleanedText.length(),
                    task.getExtractedFields() != null ? task.getExtractedFields().size() : 0);

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            task.setParseStatus("failed");
            task.setErrorMessage(e.getMessage());
            task.setUpdateTime(LocalDateTime.now());
            log.error("[{}] 解析失败 ({}ms): {}", taskId, elapsed, e.getMessage(), e);
        }
    }
}
