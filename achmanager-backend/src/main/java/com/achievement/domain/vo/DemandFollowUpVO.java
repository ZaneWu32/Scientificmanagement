package com.achievement.domain.vo;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class DemandFollowUpVO {
    private String label;
    private String owner;
    private String status;
    private LocalDateTime dueAt;
    private String note;
}
