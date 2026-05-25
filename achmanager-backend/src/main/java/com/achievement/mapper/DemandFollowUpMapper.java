package com.achievement.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.achievement.domain.po.DemandFollowUp;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface DemandFollowUpMapper extends BaseMapper<DemandFollowUp> {

    List<DemandFollowUp> selectActiveByDemandId(@Param("demandId") Long demandId);
}
