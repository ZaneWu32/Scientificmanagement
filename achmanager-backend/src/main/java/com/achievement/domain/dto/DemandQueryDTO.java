package com.achievement.domain.dto;

import lombok.Data;

@Data
public class DemandQueryDTO {
    private Integer pageNum;
    private Integer pageSize;
    private String keyword;
    private String status;
    private String industry;
    private String region;
}
