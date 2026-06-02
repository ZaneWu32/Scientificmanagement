package com.achievement.service;

import java.util.List;
import java.util.Map;

import com.achievement.domain.po.AchievementSearchDoc;
import com.achievement.domain.vo.AchievementSearchHitVO;
import com.achievement.domain.vo.RagIndexResultVO;

public interface IRagAchievementIndexService {

    Map<String, Object> elasticsearchHealth();

    void ensureAchievementIndex();

    AchievementSearchDoc rebuildAchievementDoc(String achievementDocId);

    RagIndexResultVO rebuildAndIndexAchievementDoc(String achievementDocId);

    RagIndexResultVO syncAchievementDoc(String achievementDocId);

    RagIndexResultVO rebuildAllAchievementDocs();

    RagIndexResultVO indexPendingAchievementDocs(Integer limit);

    RagIndexResultVO rebuildAndIndexAllAchievementDocs();

    List<AchievementSearchHitVO> searchAchievements(String keyword, Integer topK);
}
