package com.achievement.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class DemandMatchVO {
    private String matchId;
    private String resultId;
    private String resultTitle;
    private String resultType;
    private String owner;
    private String department;
    private BigDecimal esScore;
    private BigDecimal ruleScore;
    private BigDecimal llmScore;
    private BigDecimal matchScore;
    private String reason;
    private String sourceSnippet;
    private List<String> fitTags;
    private String confirmStatus;
    private LocalDateTime updatedAt;
}
