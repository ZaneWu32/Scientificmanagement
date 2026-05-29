package com.achievement.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.achievement.domain.vo.AutoFillResultVO;
import com.achievement.result.Result;
import com.achievement.service.IAutoFillService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auto-fill")
@RequiredArgsConstructor
@Tag(name = "附件智能识别接口")
public class AutoFillFromAttachmentController {

    private final IAutoFillService autoFillService;

    @Operation(description = "从附件中识别成果元数据字段")
    @PostMapping("/from-attachment")
    public Result<AutoFillResultVO> fromAttachment(
            @RequestParam("file") MultipartFile file,
            @RequestParam("resultTypeCode") String resultTypeCode) {
        return Result.success(autoFillService.recognizeFields(file, resultTypeCode));
    }
}
