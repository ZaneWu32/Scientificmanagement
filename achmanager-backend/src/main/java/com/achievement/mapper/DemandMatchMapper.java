package com.achievement.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.achievement.domain.po.DemandMatch;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface DemandMatchMapper extends BaseMapper<DemandMatch> {

    @Select("SELECT COUNT(DISTINCT demand_id) FROM demand_matches WHERE is_delete = 0")
    Long countMatchedDemands();

    List<DemandMatch> selectActiveByDemandId(@Param("demandId") Long demandId);

    int softDeleteUnconfirmedByDemandId(@Param("demandId") Long demandId);

    /**
     * 排除某条候选成果匹配结果（标记为 rejected）
     *
     * @param demandId  需求 ID
     * @param resultId  候选成果 matchId (demand_matches.id) 或 achievementDocId
     * @param rejectedBy 操作人名称
     * @return 更新行数
     */
    int rejectMatch(@Param("demandId") Long demandId,
                    @Param("resultId") String resultId,
                    @Param("rejectedBy") String rejectedBy);
}
