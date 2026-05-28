package com.achievement.service;

import com.achievement.domain.vo.PolicyVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

public interface ICrawlerPolicyService {

    void syncAllCrawlers();

    void syncCrawler(String crawlerId);

    void matchPoliciesWithAchievements();

    List<PolicyVO> getRelatedPolicies(String achievementDocId, int limit);

    IPage<PolicyVO> getAllPolicies(int page, int pageSize);
}
