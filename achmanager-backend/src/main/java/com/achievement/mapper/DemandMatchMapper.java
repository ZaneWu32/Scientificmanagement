package com.achievement.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.achievement.domain.po.DemandMatch;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface DemandMatchMapper extends BaseMapper<DemandMatch> {

    List<DemandMatch> selectActiveByDemandId(@Param("demandId") Long demandId);

    int softDeleteUnconfirmedByDemandId(@Param("demandId") Long demandId);
}
