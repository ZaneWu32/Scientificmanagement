package com.achievement.domain.dto;

import lombok.Data;

@Data
public class PolicyQueryDTO {
    private String keyword;
    private String crawlerId;
    private String startDate;
    private String endDate;
    private int page = 1;
    private int pageSize = 20;
}
