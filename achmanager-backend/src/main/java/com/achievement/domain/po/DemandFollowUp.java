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
@TableName("demand_follow_ups")
public class DemandFollowUp implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long demandId;
    private String ownerId;
    private String ownerName;
    private String status;
    private String nextAction;
    private LocalDateTime dueAt;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDelete;
}
