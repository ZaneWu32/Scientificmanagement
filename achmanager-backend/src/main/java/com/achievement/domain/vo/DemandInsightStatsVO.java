package com.achievement.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "需求洞察指标统计响应")
public class DemandInsightStatsVO {

    @Schema(description = "入池需求总数")
    private Long totalDemands;

    @Schema(description = "有候选成果需求数")
    private Long matchedDemands;

    @Schema(description = "跟进中需求数")
    private Long followUpDemands;
}
