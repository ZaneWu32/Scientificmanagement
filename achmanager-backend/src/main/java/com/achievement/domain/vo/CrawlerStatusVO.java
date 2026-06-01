package com.achievement.domain.vo;

import lombok.Data;

@Data
public class CrawlerStatusVO {
    private String id;
    private String name;
    private String status;
    private long policyCount;
    private CrawlerStatsVO stats;
}
