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
@TableName("demand_sources")
public class DemandSource implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String name;
    private String type;
    private String industry;
    private String region;
    private String baseUrl;
    private String description;
    private String authType;
    private String credentialsJson;
    private Integer frequencyHours;
    private String priority;
    private String tagsJson;
    private Boolean enabled;
    private String status;
    private LocalDateTime lastRunAt;
    private LocalDateTime lastSuccessAt;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer isDelete;
}
