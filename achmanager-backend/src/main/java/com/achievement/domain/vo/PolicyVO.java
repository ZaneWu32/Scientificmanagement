package com.achievement.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PolicyVO {
    private Long id;
    private String crawlerId;
    private String title;
    private String publishDate;
    private String sourceUrl;
    private String contentPreview;
    private List<String> hrefs;
    private BigDecimal matchScore;
    private String matchReason;
}
