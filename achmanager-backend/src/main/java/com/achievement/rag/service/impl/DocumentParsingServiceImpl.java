package com.achievement.rag.service.impl;

import com.achievement.config.ProcessSystemProperties;
import com.achievement.domain.vo.AchTypeDetailVO;
import com.achievement.rag.model.*;
import com.achievement.rag.service.IDocumentParsingService;
import com.achievement.service.IAchievementTypesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文档解析服务实现
 * <p>
 * 使用 ConcurrentHashMap 内存管理任务状态，使用 DocumentParseTaskProcessor 异步处理。
 * </p>
 */
@Slf4j
@Service
public class DocumentParsingServiceImpl implements IDocumentParsingService {

    private final Map<String, ParseTask> taskStore = new ConcurrentHashMap<>();
    private static final String RAG_UPLOAD_DIR = "rag_uploads";

    private final ProcessSystemProperties processSystemProperties;
    private final IAchievementTypesService achievementTypesService;
    private final DocumentParseTaskProcessor taskProcessor;

    public DocumentParsingServiceImpl(ProcessSystemProperties processSystemProperties,
                                      IAchievementTypesService achievementTypesService,
                                      DocumentParseTaskProcessor taskProcessor) {
        this.processSystemProperties = processSystemProperties;
        this.achievementTypesService = achievementTypesService;
        this.taskProcessor = taskProcessor;
    }

    @Override
    public ParseRequestVO uploadAndParse(MultipartFile file, String typeDocId,
                                         String userId, String userName) {
        String fileId = "rag_" + UUID.randomUUID().toString().replace("-", "");
        String originalName = file.getOriginalFilename();
        String mimeType = resolveMimeType(originalName);

        // 1. 保存文件到 rag_uploads 目录
        String storagePath;
        try {
            storagePath = storeUploadFile(file, fileId);
        } catch (IOException e) {
            throw new RuntimeException("文件保存失败: " + e.getMessage(), e);
        }

        // 2. 创建任务
        ParseTask task = new ParseTask();
        task.setTaskId(fileId);
        task.setFileId(fileId);
        task.setFileName(originalName);
        task.setOriginalName(originalName);
        task.setFileSize(file.getSize());
        task.setMimeType(mimeType);
        task.setFilePath(storagePath);
        task.setTypeDocId(typeDocId);
        task.setParseStatus("queued");

        // 3. 获取成果物类型的字段定义（同步获取，便于快速返回）
        try {
            AchTypeDetailVO typeDetail = achievementTypesService.selectDetail(typeDocId);
            task.setTypeCode(typeDetail.getTypeCode());
            task.setTypeName(typeDetail.getTypeName());
            task.setFieldDefinitions(typeDetail.getFieldDefinitions());
        } catch (Exception e) {
            log.warn("获取成果物类型定义失败: {}", e.getMessage());
            task.setFieldDefinitions(Collections.emptyList());
        }

        taskStore.put(fileId, task);
        log.info("Parse task created: taskId={}, file={}, type={}", fileId, originalName, typeDocId);

        // 4. 异步处理（由独立的 DocumentParseTaskProcessor 执行，确保 @Async 生效）
        taskProcessor.processTask(fileId);

        return ParseRequestVO.builder()
                .taskId(fileId)
                .fileId(fileId)
                .fileName(originalName)
                .parseStatus(task.getParseStatus())
                .build();
    }

    @Override
    public ParseResultVO getParseResult(String taskId) {
        ParseTask task = taskStore.get(taskId);
        if (task == null) return null;

        return ParseResultVO.builder()
                .taskId(task.getTaskId())
                .parseStatus(task.getParseStatus())
                .errorMessage(task.getErrorMessage())
                .typeCode(task.getTypeCode())
                .typeName(task.getTypeName())
                .fieldDefinitions(task.getFieldDefinitions())
                .extractedFields(task.getExtractedFields())
                .build();
    }

    @Override
    public ParseTask getTask(String taskId) {
        return taskStore.get(taskId);
    }

    // ==================== 文件存储 ====================

    private String storeUploadFile(MultipartFile file, String fileId) throws IOException {
        String basePath = processSystemProperties.getFileStorage().getBasePath();
        String ext = getExtension(file.getOriginalFilename());
        String fileName = fileId + (ext != null ? "." + ext : "");

        String datePath = java.time.LocalDate.now().toString().replace("-", "/");
        String relativePath = RAG_UPLOAD_DIR + "/" + datePath + "/" + fileName;

        Path targetPath = Paths.get(basePath, relativePath);
        Files.createDirectories(targetPath.getParent());
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        log.info("RAG file stored: {} -> {}", file.getOriginalFilename(), targetPath);
        return relativePath;
    }

    private String getExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) return null;
        int idx = fileName.lastIndexOf('.');
        return idx > 0 ? fileName.substring(idx + 1).toLowerCase() : null;
    }

    private String resolveMimeType(String fileName) {
        if (fileName == null) return "application/octet-stream";
        String ext = getExtension(fileName);
        if (ext == null) return "application/octet-stream";
        return switch (ext) {
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt" -> "application/vnd.ms-powerpoint";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "txt" -> "text/plain";
            default -> "application/octet-stream";
        };
    }
}
