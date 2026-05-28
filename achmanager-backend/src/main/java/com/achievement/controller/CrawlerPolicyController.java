package com.achievement.controller;

import com.achievement.domain.vo.PolicyVO;
import com.achievement.result.Result;
import com.achievement.service.ICrawlerPolicyService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping("/sync")
    public Result<String> triggerSync() {
        log.info("手动触发爬虫数据同步");
        try {
            crawlerPolicyService.syncAllCrawlers();
            return Result.success("同步完成");
        } catch (Exception e) {
            log.error("爬虫同步失败", e);
            return Result.error("同步失败: " + e.getMessage());
        }
    }

    @PostMapping("/match")
    public Result<String> triggerMatch() {
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
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(crawlerPolicyService.getAllPolicies(page, pageSize));
    }
}
