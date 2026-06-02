package com.achievement.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.achievement.annotation.CurrentUser;
import com.achievement.domain.dto.KeycloakUser;
import com.achievement.domain.po.AchievementSearchDoc;
import com.achievement.domain.vo.AchievementSearchHitVO;
import com.achievement.domain.vo.RagIndexResultVO;
import com.achievement.result.Result;
import com.achievement.service.IRagAchievementIndexService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/rag")
@RequiredArgsConstructor
@Tag(name = "RAG 检索与索引接口")
public class RagController {

    private final IRagAchievementIndexService ragAchievementIndexService;

    @Operation(description = "查看 Elasticsearch 连接状态")
    @GetMapping("/es/health")
    public Result<Map<String, Object>> elasticsearchHealth() {
        return Result.success(ragAchievementIndexService.elasticsearchHealth());
    }

    @Operation(description = "创建或确认成果 ES 索引")
    @PostMapping("/achievement-index/ensure")
    public Result<String> ensureAchievementIndex(@CurrentUser KeycloakUser currentUser) {
        if (!isAdmin(currentUser)) {
            return Result.error(403, "无权限：仅管理员可操作索引");
        }
        ragAchievementIndexService.ensureAchievementIndex();
        return Result.success("ok");
    }

    @Operation(description = "重建全部已审核成果的 MySQL 检索快照")
    @PostMapping("/search-docs/achievements/rebuild")
    public Result<RagIndexResultVO> rebuildAllAchievementDocs(@CurrentUser KeycloakUser currentUser) {
        if (!isAdmin(currentUser)) {
            return Result.error(403, "无权限：仅管理员可重建索引快照");
        }
        return Result.success(ragAchievementIndexService.rebuildAllAchievementDocs());
    }

    @Operation(description = "重建单个成果的 MySQL 检索快照")
    @PostMapping("/search-docs/achievements/{achievementDocId}")
    public Result<AchievementSearchDoc> rebuildAchievementDoc(@PathVariable String achievementDocId,
                                                              @CurrentUser KeycloakUser currentUser) {
        if (!isAdmin(currentUser)) {
            return Result.error(403, "无权限：仅管理员可重建索引快照");
        }
        return Result.success(ragAchievementIndexService.rebuildAchievementDoc(achievementDocId));
    }

    @Operation(description = "同步单个成果的 RAG 索引；已审核成果会重建并写入 ES，未审核/删除成果会清理索引")
    @PostMapping("/search-docs/achievements/{achievementDocId}/sync")
    public Result<RagIndexResultVO> syncAchievementDoc(@PathVariable String achievementDocId,
                                                       @CurrentUser KeycloakUser currentUser) {
        if (!isAdmin(currentUser)) {
            return Result.error(403, "无权限：仅管理员可同步索引");
        }
        return Result.success(ragAchievementIndexService.syncAchievementDoc(achievementDocId));
    }

    @Operation(description = "将待同步的成果检索快照写入 ES")
    @PostMapping("/search-docs/achievements/index")
    public Result<RagIndexResultVO> indexPendingAchievementDocs(@RequestParam(required = false) Integer limit,
                                                               @CurrentUser KeycloakUser currentUser) {
        if (!isAdmin(currentUser)) {
            return Result.error(403, "无权限：仅管理员可写入索引");
        }
        return Result.success(ragAchievementIndexService.indexPendingAchievementDocs(limit));
    }

    @Operation(description = "重建全部成果快照并写入 ES")
    @PostMapping("/search-docs/achievements/rebuild-and-index")
    public Result<RagIndexResultVO> rebuildAndIndexAllAchievementDocs(@CurrentUser KeycloakUser currentUser) {
        if (!isAdmin(currentUser)) {
            return Result.error(403, "无权限：仅管理员可重建并写入索引");
        }
        return Result.success(ragAchievementIndexService.rebuildAndIndexAllAchievementDocs());
    }

    @Operation(description = "ES 成果检索调试接口")
    @GetMapping("/achievements/search")
    public Result<List<AchievementSearchHitVO>> searchAchievements(@RequestParam String keyword,
                                                                   @RequestParam(required = false) Integer topK) {
        return Result.success(ragAchievementIndexService.searchAchievements(keyword, topK));
    }

    private boolean isAdmin(KeycloakUser currentUser) {
        return currentUser != null && currentUser.hasRole("research_admin");
    }
}
