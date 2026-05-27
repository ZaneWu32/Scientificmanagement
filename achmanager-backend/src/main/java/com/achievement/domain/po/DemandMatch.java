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
@TableName("demand_matches")
public class DemandMatch implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long demandId;
    private String achievementDocId;
    private String resultTitle;
    private String resultType;
    private String owner;
    private String department;
    private BigDecimal esScore;
    private BigDecimal ruleScore;
    private BigDecimal llmScore;
    private BigDecimal matchScore;
    private String reason;
    private String sourceSnippet;
    private String fitTagsJson;
    private String confirmStatus;
    private String confirmedBy;
    private LocalDateTime confirmedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDelete;
}
