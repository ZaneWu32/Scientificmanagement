package com.achievement.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.achievement.domain.po.AchievementSearchDoc;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface AchievementSearchDocMapper extends BaseMapper<AchievementSearchDoc> {

    List<String> selectApprovedAchievementDocIds();

    List<AchievementSearchDoc> selectPendingEsDocs(@Param("limit") int limit);

    int markEsIndexed(@Param("id") Long id,
                      @Param("esDocId") String esDocId,
                      @Param("esIndexedAt") LocalDateTime esIndexedAt);
}
