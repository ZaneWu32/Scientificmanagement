package com.achievement.domain.dto;

import java.util.List;

import lombok.Data;

@Data
public class DemandMatchPreviewRequest {
    private String title;
    private String content;
    private String industry;
    private String region;
    private List<String> keywords;
    private Integer topK;
}
