package com.achievement.controller;

import com.achievement.annotation.CurrentUser;
import com.achievement.domain.dto.KeycloakUser;
import com.achievement.domain.vo.CrawlerStatusVO;
import com.achievement.domain.vo.PolicyVO;
import com.achievement.result.Result;
import com.achievement.service.ICrawlerPolicyService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/policy")
@RequiredArgsConstructor
public class CrawlerPolicyController {

    private final ICrawlerPolicyService crawlerPolicyService;

    @GetMapping("/achievement/{achievementDocId}/related")
    public Result<List<PolicyVO>> getRelatedPolicies(
            @PathVariable String achievementDocId,
            @RequestParam(defaultValue = "5") int limit) {
        return Result.success(crawlerPolicyService.getRelatedPolicies(achievementDocId, limit));
    }

    @GetMapping("/crawlers")
    public Result<List<CrawlerStatusVO>> getCrawlerStatus(@CurrentUser KeycloakUser currentUser) {
        if (!currentUser.hasRole("research_admin")) {
            return Result.error("无权限：仅管理员可访问");
        }
        return Result.success(crawlerPolicyService.getCrawlerStatusList());
    }

    @PostMapping("/sync")
    public Result<String> triggerSync(@CurrentUser KeycloakUser currentUser) {
        if (!currentUser.hasRole("research_admin")) {
            return Result.error("无权限：仅管理员可访问");
        }
        log.info("手动触发全部爬虫同步");
        try {
            crawlerPolicyService.syncAllCrawlers();
            return Result.success("同步完成");
        } catch (Exception e) {
            log.error("爬虫同步失败", e);
            return Result.error("同步失败: " + e.getMessage());
        }
    }

    @PostMapping("/crawler/{crawlerId}/sync")
    public Result<String> triggerCrawlerSync(@PathVariable String crawlerId, @CurrentUser KeycloakUser currentUser) {
        if (!currentUser.hasRole("research_admin")) {
            return Result.error("无权限：仅管理员可访问");
        }
        log.info("手动触发爬虫 {} 同步", crawlerId);
        try {
            crawlerPolicyService.syncCrawler(crawlerId);
            return Result.success("同步完成");
        } catch (Exception e) {
            log.error("爬虫 {} 同步失败", crawlerId, e);
            return Result.error("同步失败: " + e.getMessage());
        }
    }

    @PostMapping("/match")
    public Result<String> triggerMatch(@CurrentUser KeycloakUser currentUser) {
        if (!currentUser.hasRole("research_admin")) {
            return Result.error("无权限：仅管理员可访问");
        }
        log.info("手动触发政策-成果物匹配");
        try {
            crawlerPolicyService.matchPoliciesWithAchievements();
            return Result.success("匹配完成");
        } catch (Exception e) {
            log.error("政策匹配失败", e);
            return Result.error("匹配失败: " + e.getMessage());
        }
    }

    @GetMapping("/list")
    public Result<IPage<PolicyVO>> listPolicies(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @CurrentUser KeycloakUser currentUser) {
        if (!currentUser.hasRole("research_admin")) {
            return Result.error("无权限：仅管理员可访问");
        }
        return Result.success(crawlerPolicyService.getAllPolicies(page, pageSize));
    }
}
