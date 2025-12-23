package com.achievement.mapper;

import com.achievement.domain.dto.AchListDTO;
import com.achievement.domain.po.AchievementMains;
import com.achievement.domain.vo.AchListVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author author
 * @since 2025-12-11
 */
public interface AchievementMainsMapper extends BaseMapper<AchievementMains> {

    /**
     * 管理员分页查询成果物列表（带类型名、创建者名等）
     */
    Page<AchListVO> pageList(Page<?> page, @Param("dto") AchListDTO dto);
}
