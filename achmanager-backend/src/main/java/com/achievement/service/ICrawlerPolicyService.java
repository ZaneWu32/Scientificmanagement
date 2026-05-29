package com.achievement.service;

import com.achievement.domain.dto.PolicyQueryDTO;
import com.achievement.domain.vo.CrawlerStatusVO;
import com.achievement.domain.vo.PolicyVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;
import java.util.Map;

public interface ICrawlerPolicyService {

    void syncAllCrawlers();

    void syncCrawler(String crawlerId);

    void triggerCrawlerSync(String crawlerId);

    void triggerSyncAll();

    void matchPoliciesWithAchievements();

    List<PolicyVO> getRelatedPolicies(String achievementDocId, int limit);

    IPage<PolicyVO> getAllPolicies(int page, int pageSize);

    IPage<PolicyVO> queryPolicies(PolicyQueryDTO dto);

    Map<String, String> getCrawlerNames();

    List<CrawlerStatusVO> getCrawlerStatusList();
}
