package com.achievement.domain.po;

import java.io.Serializable;
import java.math.BigDecimal;
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
@TableName("demand_items")
public class DemandItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String demandCode;
    private String title;
    private String rawContent;
    private String summary;
    private String llmSummary;
    private String keywordsJson;
    private String tagsJson;
    private String industry;
    private String region;
    private String sourceCategory;
    private Long sourceId;
    private String sourceSite;
    private String sourceUrl;
    private LocalDateTime capturedAt;
    private String priority;
    private BigDecimal confidence;
    private BigDecimal bestMatchScore;
    private String status;
    private String ownerId;
    private String ownerName;
    private LocalDateTime dueAt;
    private String pendingConfirmationsJson;
    private String riskNotesJson;
    private String contentHash;
    private String esDocId;
    private LocalDateTime esIndexedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDelete;
}
