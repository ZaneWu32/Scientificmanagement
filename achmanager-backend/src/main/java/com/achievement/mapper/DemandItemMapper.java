package com.achievement.mapper;

import org.apache.ibatis.annotations.Param;

import com.achievement.domain.po.DemandItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface DemandItemMapper extends BaseMapper<DemandItem> {

    Page<DemandItem> pageList(Page<?> page,
                              @Param("keyword") String keyword,
                              @Param("status") String status,
                              @Param("industry") String industry,
                              @Param("region") String region);
}
