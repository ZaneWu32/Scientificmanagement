package com.achievement.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 成果全文访问授权表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("achievement_access_grants")
@Schema(name = "AchievementAccessGrant对象", description = "成果全文访问授权")
public class AchievementAccessGrant implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String documentId;

    private Integer achievementId;

    private String achievementDocId;

    private Integer granteeId;

    private String granteeName;

    private Integer sourceRequestId;

    private String sourceRequestDocId;

    private Integer grantedById;

    private String grantedByName;

    private String grantScope;

    private String status;

    private LocalDateTime grantedAt;

    private LocalDateTime expiresAt;

    private LocalDateTime revokedAt;

    private String revokeReason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime publishedAt;

    private Integer createdById;

    private Integer updatedById;

    private String locale;

    private Integer isDelete;
}
