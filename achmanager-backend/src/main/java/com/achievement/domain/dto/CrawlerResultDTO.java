package com.achievement.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class CrawlerResultDTO {
    private String source;
    private String title;
    private String datetime;
    private String content;
    private List<String> hrefs;
}
