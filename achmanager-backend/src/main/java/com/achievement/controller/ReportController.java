package com.achievement.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;

import com.achievement.annotation.CurrentUser;
import com.achievement.constant.RoleConstants;
import com.achievement.domain.dto.KeycloakUser;
import com.achievement.domain.dto.ReportGenerateRequest;
import com.achievement.domain.vo.ReportTaskVO;
import com.achievement.result.Result;
import com.achievement.service.IReportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "智能报告导出")
@RestController
@RequestMapping("/admin/report")
@RequiredArgsConstructor
public class ReportController {

    private final IReportService reportService;

    private void checkAdminRole(KeycloakUser currentUser) {
        if (!currentUser.hasRole(RoleConstants.RESEARCH_ADMIN)) {
            throw new HttpClientErrorException(HttpStatusCode.valueOf(403), "无权限：仅管理员可访问");
        }
    }

    @Operation(summary = "开始生成报告")
    @PostMapping("/generate")
    public Result<Map<String, String>> generate(@Valid @RequestBody ReportGenerateRequest request,
            @CurrentUser KeycloakUser currentUser) {
        checkAdminRole(currentUser);
        String taskId = reportService.startGeneration(request.getAchievementDocIds());
        return Result.success(Map.of("taskId", taskId));
    }

    @Operation(summary = "查询任务状态")
    @GetMapping("/task/{taskId}")
    public Result<ReportTaskVO> getTaskStatus(@PathVariable String taskId, @CurrentUser KeycloakUser currentUser) {
        checkAdminRole(currentUser);
        ReportTaskVO vo = reportService.getTaskStatus(taskId);
        if (vo == null) {
            return Result.error("任务不存在");
        }
        return Result.success(vo);
    }

    @Operation(summary = "获取报告HTML内容")
    @GetMapping("/task/{taskId}/content")
    public Result<Map<String, String>> getContent(@PathVariable String taskId, @CurrentUser KeycloakUser currentUser) {
        checkAdminRole(currentUser);
        String html = reportService.getTaskContent(taskId);
        if (html == null) {
            return Result.error("报告内容未就绪");
        }
        return Result.success(Map.of("html", html));
    }

    @Operation(summary = "导出报告文件")
    @PostMapping("/task/{taskId}/export")
    public ResponseEntity<byte[]> export(
            @PathVariable String taskId,
            @RequestBody Map<String, String> body, @CurrentUser KeycloakUser currentUser) {
        checkAdminRole(currentUser);

        String format = body.getOrDefault("format", "word");
        String editedHtml = body.get("html");

        byte[] data = reportService.exportReport(taskId, format, editedHtml);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "科研成果报告_" + timestamp + ".docx";

        MediaType mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encodedFilename)
                .contentType(mediaType)
                .body(data);
    }
}
