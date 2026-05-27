package com.achievement.domain.vo;

import lombok.Data;

@Data
public class AchievementSearchHitVO {
    private String achievementDocId;
    private String title;
    private String typeName;
    private String typeCode;
    private String summary;
    private String keywordsText;
    private String authorsText;
    private String projectName;
    private String year;
    private String visibilityRange;
    private String attachmentNames;
    private String sourceSnippet;
    private Double esScore;
}
