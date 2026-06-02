package com.achievement.rag.controller;

import com.achievement.result.Result;
import com.achievement.rag.model.ParseRequestVO;
import com.achievement.rag.model.ParseResultVO;
import com.achievement.rag.service.IDocumentParsingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * RAG 文档解析与字段提取接口
 * <p>
 * 流程：
 * 1. 前端先调用 /achievementType/list 获取成果物类型列表
 * 2. 调用 /achievementType/detail?typeDocId=xxx 获取该类型的字段定义
 * 3. 用户选择文件后调用 POST /api/rag/parse 上传（需指定 typeDocId）
 * 4. 前端轮询 GET /api/rag/parse/{taskId} 获取解析和提取结果
 * 5. 提取的字段按 fieldDefinitions 的结构返回，前端据此渲染表单供用户确认
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
@Tag(name = "RAG 文档解析与字段提取")
public class RagController {

    private final IDocumentParsingService documentParsingService;

    @Operation(summary = "上传文件并解析提取字段",
            description = "上传成果物文件+指定成果物类型，异步解析文档并提取结构化字段")
    @PostMapping(value = "/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<ParseRequestVO> uploadAndParse(
            @RequestParam("file") MultipartFile file,
            @RequestParam("typeDocId") String typeDocId,
            @RequestParam(value = "userId", required = false, defaultValue = "system") String userId,
            @RequestParam(value = "userName", required = false, defaultValue = "System") String userName) {

        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            return Result.error("文件名不能为空");
        }

        // 校验文件类型
        String ext = getExtension(fileName);
        if (!isAllowedExtension(ext)) {
            return Result.error("不支持的文件格式: " + ext + "（支持 pdf/doc/docx/xls/xlsx/ppt/pptx/txt）");
        }

        try {
            ParseRequestVO result = documentParsingService.uploadAndParse(file, typeDocId, userId, userName);
            return Result.success(result);
        } catch (Exception e) {
            log.error("上传解析失败: {}", e.getMessage(), e);
            return Result.error("上传解析失败: " + e.getMessage());
        }
    }

    @Operation(summary = "查询解析任务结果",
            description = "轮询此接口获取异步解析任务的当前状态和提取的字段数据")
    @GetMapping("/parse/{taskId}")
    public Result<ParseResultVO> getParseResult(@PathVariable String taskId) {
        ParseResultVO result = documentParsingService.getParseResult(taskId);
        if (result == null) {
            return Result.error("任务不存在或已过期");
        }
        return Result.success(result);
    }

    // ==================== 辅助 ====================

    private static final String[] ALLOWED_EXTS = {"pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt"};

    private boolean isAllowedExtension(String ext) {
        if (ext == null) return false;
        for (String allowed : ALLOWED_EXTS) {
            if (allowed.equalsIgnoreCase(ext)) return true;
        }
        return false;
    }

    private String getExtension(String fileName) {
        if (fileName == null) return null;
        int idx = fileName.lastIndexOf('.');
        return idx > 0 ? fileName.substring(idx + 1).toLowerCase() : null;
    }
}
