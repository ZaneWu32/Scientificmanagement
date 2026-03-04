package com.achievement.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 访问申请创建 DTO
 */
@Data
@Schema(description = "访问申请创建")
public class AccessRequestCreateDTO {

    @NotBlank(message = "申请理由不能为空")
    @Schema(description = "申请理由")
    private String reason;
}
