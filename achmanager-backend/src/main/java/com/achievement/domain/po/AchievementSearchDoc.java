package com.achievement.domain.po;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("achievement_search_docs")
public class AchievementSearchDoc implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

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
    private String attachmentTextPreview;
    private String attachmentExtractStatus;
    private String attachmentExtractError;
    private String searchText;
    private String searchHash;
    private String esDocId;
    private LocalDateTime esIndexedAt;
    private LocalDateTime indexedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDelete;
}
