package com.achievement.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "报告任务状态")
public class ReportTaskVO {

    @Schema(description = "任务ID")
    private String taskId;

    @Schema(description = "状态: PENDING, GENERATING, COMPLETED, FAILED")
    private String status;

    @Schema(description = "进度 0-100")
    private int progress;

    @Schema(description = "错误信息")
    private String errorMsg;
}
