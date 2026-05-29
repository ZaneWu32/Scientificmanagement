package com.achievement.domain.vo;

import lombok.Data;

@Data
public class CrawlerStatusVO {
    private String id;
    private String name;
    private String syncStatus;
    private String crawlerStatus;
}
