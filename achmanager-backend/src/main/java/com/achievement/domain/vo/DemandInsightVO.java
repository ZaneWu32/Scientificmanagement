package com.achievement.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class DemandInsightVO {
    private Long id;
    private String demandCode;
    private String title;
    private String sourceCategory;
    private String sourceSite;
    private String sourceUrl;
    private LocalDateTime capturedAt;
    private String industry;
    private String region;
    private String priority;
    private BigDecimal confidence;
    private BigDecimal bestMatchScore;
    private String status;
    private String valueLevel;
    private String owner;
    private LocalDateTime dueAt;
    private String summary;
    private String llmSummary;
    private List<String> keywords;
    private List<String> tags;
    private List<String> pendingConfirmations;
    private List<String> riskNotes;
    private List<DemandMatchVO> matches;
    private List<DemandFollowUpVO> followUp;
}
