package com.achievement.domain.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
@Schema(description = "报告生成请求")
public class ReportGenerateRequest {

    @NotEmpty(message = "成果物ID列表不能为空")
    @Schema(description = "成果物documentId列表")
    private List<String> achievementDocIds;
}
