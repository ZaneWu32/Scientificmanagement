package com.achievement.domain.vo;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class DemandSourceVO {
    private Long id;
    private String name;
    private String type;
    private String industry;
    private String region;
    private Integer frequencyHours;
    private Boolean enabled;
    private String status;
    private LocalDateTime lastRunAt;
    private LocalDateTime lastSuccessAt;
    private String failureReason;
    private Integer successRate;
    private Integer newCount;
    private Integer matchedCount;
}
