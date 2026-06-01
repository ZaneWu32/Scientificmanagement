package com.achievement.domain.vo;

import lombok.Data;

@Data
public class CrawlerStatsVO {
    private int total;
    private int success;
    private int failed;
    private int done;
}
