package com.achievement.domain.dto;

import lombok.Data;

/**
 * 管理员查询成果物列表的查询条件
 */
@Data

public class AchListDTO {

    /** 页码，从 1 开始 */
    private Integer pageNum = 1;

    /** 每页大小 */
    private Integer pageSize = 10;

    /** 按成果物名称模糊搜索 */
    private String title;

    /** 成果物类型ID（achievement_types.id） */
    private Integer typeId;

    /** 审核状态（achievement_status） */
    private String status;

    /** 创建者用户名（模糊匹配 admin_users.username） */
    private String creatorName;

    /** 项目名（后面你加了项目表再用） */
    private String projectName;

    /** 可见范围（后面你加字段再用） */
    private String visibleScope;
}
