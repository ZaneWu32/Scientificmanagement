package com.achievement.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("crawler_policy_achievement_match")
public class CrawlerPolicyAchievementMatch implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long policyId;

    private String achievementDocumentId;

    private BigDecimal matchScore;

    private String matchMethod;

    private String matchReason;

    private Integer isActive;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
