package com.achievement.mapper;

import java.util.List;

import com.achievement.domain.po.DemandSource;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface DemandSourceMapper extends BaseMapper<DemandSource> {

    List<DemandSource> selectActiveSources();
}
