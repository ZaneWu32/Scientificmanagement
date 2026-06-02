package com.achievement.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.achievement.domain.dto.DemandMatchPreviewRequest;
import com.achievement.domain.dto.DemandQueryDTO;
import com.achievement.domain.dto.DemandStatusUpdateDTO;
import com.achievement.domain.dto.KeycloakUser;
import com.achievement.domain.po.DemandFollowUp;
import com.achievement.domain.po.DemandItem;
import com.achievement.domain.po.DemandMatch;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.achievement.domain.po.DemandSource;
import com.achievement.domain.vo.AchievementSearchHitVO;
import com.achievement.domain.vo.DemandFollowUpVO;
import com.achievement.domain.vo.DemandInsightStatsVO;
import com.achievement.domain.vo.DemandInsightVO;
import com.achievement.domain.vo.DemandMatchVO;
import com.achievement.domain.vo.DemandSourceVO;
import com.achievement.mapper.DemandFollowUpMapper;
import com.achievement.mapper.DemandItemMapper;
import com.achievement.mapper.DemandMatchMapper;
import com.achievement.mapper.DemandSourceMapper;
import com.achievement.service.IDemandMatchLlmRerankService;
import com.achievement.service.IDemandInsightService;
import com.achievement.service.IRagAchievementIndexService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemandInsightServiceImpl implements IDemandInsightService {

    private static final Set<String> ALLOWED_STATUS = Set.of(
            "new", "reviewing", "matched", "in_follow_up", "invalid", "archived");

    private final DemandItemMapper demandItemMapper;
    private final DemandMatchMapper demandMatchMapper;
    private final DemandSourceMapper demandSourceMapper;
    private final DemandFollowUpMapper demandFollowUpMapper;
    private final IRagAchievementIndexService ragAchievementIndexService;
    private final IDemandMatchLlmRerankService demandMatchLlmRerankService;
    private final ObjectMapper objectMapper;

    @Override
    public DemandInsightStatsVO getStats() {
        DemandInsightStatsVO stats = new DemandInsightStatsVO();
        long total = demandItemMapper.selectCount(new LambdaQueryWrapper<DemandItem>()
                .eq(DemandItem::getIsDelete, 0));
        long matched = demandMatchMapper.countMatchedDemands();
        long followUp = demandItemMapper.selectCount(new LambdaQueryWrapper<DemandItem>()
                .eq(DemandItem::getIsDelete, 0)
                .eq(DemandItem::getStatus, "in_follow_up"));
        stats.setTotalDemands(total);
        stats.setMatchedDemands(matched);
        stats.setFollowUpDemands(followUp);
        return stats;
    }

    @Override
    public Page<DemandInsightVO> pageList(DemandQueryDTO query) {
        DemandQueryDTO safeQuery = query == null ? new DemandQueryDTO() : query;
        int pageNum = safeQuery.getPageNum() == null || safeQuery.getPageNum() < 1 ? 1 : safeQuery.getPageNum();
        int pageSize = safeQuery.getPageSize() == null || safeQuery.getPageSize() < 1 ? 10 : Math.min(safeQuery.getPageSize(), 100);
        Page<DemandItem> page = new Page<>(pageNum, pageSize);
        Page<DemandItem> dbPage = demandItemMapper.pageList(
                page,
                safeQuery.getKeyword(),
                safeQuery.getStatus(),
                safeQuery.getIndustry(),
                safeQuery.getRegion());

        Page<DemandInsightVO> result = new Page<>(dbPage.getCurrent(), dbPage.getSize(), dbPage.getTotal());
        result.setRecords(dbPage.getRecords().stream().map(this::toDemandVO).toList());
        return result;
    }

    @Override
    public DemandInsightVO detail(Long id) {
        DemandItem item = demandItemMapper.selectById(id);
        if (item == null || Integer.valueOf(1).equals(item.getIsDelete())) {
            throw new RuntimeException("需求不存在或已删除");
        }
        return toDemandVO(item);
    }

    @Override
    @Transactional
    public List<DemandMatchVO> rematch(Long demandId) {
        DemandItem item = demandItemMapper.selectById(demandId);
        if (item == null || Integer.valueOf(1).equals(item.getIsDelete())) {
            throw new RuntimeException("需求不存在或已删除");
        }
        String query = buildDemandQuery(item);
        List<AchievementSearchHitVO> hits = ragAchievementIndexService.searchAchievements(query, 10);
        demandMatchMapper.softDeleteUnconfirmedByDemandId(demandId);

        double maxEsScore = hits.stream()
                .map(AchievementSearchHitVO::getEsScore)
                .filter(score -> score != null && score > 0)
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(1D);

        LocalDateTime now = LocalDateTime.now();
        List<DemandMatch> matches = new ArrayList<>();
        for (AchievementSearchHitVO hit : hits) {
            if (hit.getAchievementDocId() == null || hit.getAchievementDocId().isBlank()) {
                continue;
            }
            DemandMatch match = buildDemandMatch(item, hit, maxEsScore, now);
            matches.add(match);
        }
        matches = demandMatchLlmRerankService.rerank(item, matches, maxEsScore);
        for (DemandMatch match : matches) {
            demandMatchMapper.insert(match);
        }

        DemandItem update = new DemandItem();
        update.setId(demandId);
        update.setUpdatedAt(now);
        if (matches.isEmpty()) {
            update.setBestMatchScore(BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP));
            update.setStatus("reviewing");
        } else {
            update.setBestMatchScore(matches.get(0).getMatchScore());
            update.setStatus("matched");
        }
        demandItemMapper.updateById(update);
        return matches.stream().map(this::toMatchVO).toList();
    }

    @Override
    @Transactional
    public DemandInsightVO updateStatus(Long demandId, DemandStatusUpdateDTO dto, KeycloakUser currentUser) {
        if (dto == null || dto.getStatus() == null || !ALLOWED_STATUS.contains(dto.getStatus())) {
            throw new IllegalArgumentException("需求状态不合法");
        }
        DemandItem item = demandItemMapper.selectById(demandId);
        if (item == null || Integer.valueOf(1).equals(item.getIsDelete())) {
            throw new RuntimeException("需求不存在或已删除");
        }
        LocalDateTime now = LocalDateTime.now();
        DemandItem update = new DemandItem();
        update.setId(demandId);
        update.setStatus(dto.getStatus());
        update.setUpdatedAt(now);
        demandItemMapper.updateById(update);

        if (dto.getNote() != null && !dto.getNote().isBlank()) {
            DemandFollowUp followUp = new DemandFollowUp();
            followUp.setDemandId(demandId);
            followUp.setOwnerId(currentUser == null || currentUser.getId() == null ? null : String.valueOf(currentUser.getId()));
            followUp.setOwnerName(resolveUserName(currentUser));
            followUp.setStatus(dto.getStatus());
            followUp.setNextAction("状态更新");
            followUp.setNote(dto.getNote());
            followUp.setCreatedAt(now);
            followUp.setUpdatedAt(now);
            followUp.setIsDelete(0);
            demandFollowUpMapper.insert(followUp);
        }
        return detail(demandId);
    }

    @Override
    @Transactional
    public DemandInsightVO confirmMatch(Long demandId, String resultId, KeycloakUser currentUser) {
        if (resultId == null || resultId.isBlank()) {
            throw new IllegalArgumentException("resultId不能为空");
        }
        DemandItem item = demandItemMapper.selectById(demandId);
        if (item == null || Integer.valueOf(1).equals(item.getIsDelete())) {
            throw new RuntimeException("需求不存在或已删除");
        }
        DemandMatch target = demandMatchMapper.selectActiveByDemandId(demandId).stream()
                .filter(match -> resultId.equals(String.valueOf(match.getId())) || resultId.equals(match.getAchievementDocId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("匹配结果不存在"));
        LocalDateTime now = LocalDateTime.now();
        target.setConfirmStatus("confirmed");
        target.setConfirmedBy(resolveUserName(currentUser));
        target.setConfirmedAt(now);
        target.setUpdatedAt(now);
        demandMatchMapper.updateById(target);

        DemandItem update = new DemandItem();
        update.setId(demandId);
        update.setStatus("in_follow_up");
        update.setBestMatchScore(target.getMatchScore());
        update.setUpdatedAt(now);
        demandItemMapper.updateById(update);

        // 自动增加一条确认成果的跟进轨迹
        String adminName = resolveUserName(currentUser);
        DemandFollowUp followUp = new DemandFollowUp();
        followUp.setDemandId(demandId);
        followUp.setOwnerId(currentUser == null || currentUser.getId() == null ? null : String.valueOf(currentUser.getId()));
        followUp.setOwnerName(adminName);
        followUp.setStatus("in_follow_up");
        followUp.setNextAction("确认成果");
        followUp.setNote("管理员 " + (adminName != null ? adminName : "系统") + " 确认了候选成果：" + target.getResultTitle());
        followUp.setCreatedAt(now);
        followUp.setUpdatedAt(now);
        followUp.setIsDelete(0);
        demandFollowUpMapper.insert(followUp);

        return detail(demandId);
    }

    @Override
    @Transactional
    public DemandInsightVO rejectMatch(Long demandId, String resultId, KeycloakUser currentUser) {
        if (resultId == null || resultId.isBlank()) {
            throw new IllegalArgumentException("resultId不能为空");
        }
        DemandItem item = demandItemMapper.selectById(demandId);
        if (item == null || Integer.valueOf(1).equals(item.getIsDelete())) {
            throw new RuntimeException("需求不存在或已删除");
        }
        DemandMatch target = demandMatchMapper.selectActiveByDemandId(demandId).stream()
                .filter(match -> resultId.equals(String.valueOf(match.getId())) || resultId.equals(match.getAchievementDocId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("匹配结果不存在"));

        LocalDateTime now = LocalDateTime.now();
        int updated = demandMatchMapper.rejectMatch(demandId, resultId, resolveUserName(currentUser));
        if (updated == 0) {
            throw new RuntimeException("匹配结果不存在或已被确认/排除，无法再次操作");
        }

        // 自动增加一条排除成果的跟进轨迹
        String adminName = resolveUserName(currentUser);
        DemandFollowUp followUp = new DemandFollowUp();
        followUp.setDemandId(demandId);
        followUp.setOwnerId(currentUser == null || currentUser.getId() == null ? null : String.valueOf(currentUser.getId()));
        followUp.setOwnerName(adminName);
        followUp.setStatus(item.getStatus());
        followUp.setNextAction("排除成果");
        followUp.setNote("管理员 " + (adminName != null ? adminName : "系统") + " 排除了候选成果：" + target.getResultTitle());
        followUp.setCreatedAt(now);
        followUp.setUpdatedAt(now);
        followUp.setIsDelete(0);
        demandFollowUpMapper.insert(followUp);

        return detail(demandId);
    }

    @Override
    public List<DemandSourceVO> listSources() {
        return demandSourceMapper.selectActiveSources().stream()
                .map(this::toSourceVO)
                .toList();
    }

    @Override
    public List<DemandMatchVO> previewMatch(DemandMatchPreviewRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        String query = buildPreviewQuery(request);
        List<AchievementSearchHitVO> hits = ragAchievementIndexService.searchAchievements(query, request.getTopK());
        double maxEsScore = hits.stream()
                .map(AchievementSearchHitVO::getEsScore)
                .filter(score -> score != null && score > 0)
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(1D);

        DemandItem transientDemand = new DemandItem()
                .setTitle(request.getTitle())
                .setRawContent(request.getContent())
                .setIndustry(request.getIndustry())
                .setRegion(request.getRegion())
                .setKeywordsJson(writeStringList(request.getKeywords()));
        LocalDateTime now = LocalDateTime.now();
        List<DemandMatch> matches = hits.stream()
                .filter(this::hasAchievementDocId)
                .map(hit -> buildDemandMatch(transientDemand, hit, maxEsScore, now))
                .toList();
        return demandMatchLlmRerankService.rerank(transientDemand, matches, maxEsScore).stream()
                .map(this::toMatchVO)
                .toList();
    }

    private DemandInsightVO toDemandVO(DemandItem item) {
        DemandInsightVO vo = new DemandInsightVO();
        vo.setId(item.getId());
        vo.setDemandCode(item.getDemandCode());
        vo.setTitle(item.getTitle());
        vo.setSourceCategory(item.getSourceCategory());
        vo.setSourceSite(item.getSourceSite());
        vo.setSourceUrl(item.getSourceUrl());
        vo.setCapturedAt(item.getCapturedAt());
        vo.setIndustry(item.getIndustry());
        vo.setRegion(item.getRegion());
        vo.setPriority(priorityLabel(item.getPriority()));
        vo.setConfidence(item.getConfidence());
        vo.setBestMatchScore(item.getBestMatchScore() == null ? BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP) : item.getBestMatchScore());
        vo.setStatus(item.getStatus());
        vo.setValueLevel(valueLevel(item.getBestMatchScore()));
        vo.setOwner(item.getOwnerName());
        vo.setDueAt(item.getDueAt());
        vo.setSummary(item.getSummary());
        vo.setLlmSummary(item.getLlmSummary() == null ? item.getSummary() : item.getLlmSummary());
        vo.setKeywords(readStringList(item.getKeywordsJson()));
        vo.setTags(readStringList(item.getTagsJson()));
        vo.setPendingConfirmations(readStringList(item.getPendingConfirmationsJson()));
        vo.setRiskNotes(readStringList(item.getRiskNotesJson()));
        vo.setMatches(demandMatchMapper.selectActiveByDemandId(item.getId()).stream().map(this::toMatchVO).toList());
        vo.setFollowUp(demandFollowUpMapper.selectActiveByDemandId(item.getId()).stream().map(this::toFollowUpVO).toList());
        return vo;
    }

    private DemandMatch buildDemandMatch(DemandItem item, AchievementSearchHitVO hit, double maxEsScore, LocalDateTime now) {
        List<String> keywords = readStringList(item.getKeywordsJson());
        List<String> hitKeywords = matchedKeywords(keywords, hit);
        double esNormalized = hit.getEsScore() == null || maxEsScore <= 0 ? 0D : hit.getEsScore() / maxEsScore;
        double ruleScore = calculateRuleScore(item, hit, hitKeywords);
        double matchScore = Math.min(0.99D, esNormalized * 0.75D + ruleScore * 0.25D);
        List<String> fitTags = buildFitTags(hit, hitKeywords);

        DemandMatch match = new DemandMatch();
        match.setDemandId(item.getId());
        match.setAchievementDocId(hit.getAchievementDocId());
        match.setResultTitle(hit.getTitle());
        match.setResultType(hit.getTypeName());
        match.setOwner(hit.getAuthorsText());
        match.setDepartment(hit.getProjectName());
        match.setEsScore(decimal(hit.getEsScore() == null ? 0D : hit.getEsScore(), 4));
        match.setRuleScore(decimal(ruleScore, 3));
        match.setMatchScore(decimal(matchScore, 3));
        match.setReason(buildReason(hitKeywords, fitTags));
        match.setSourceSnippet(hit.getSourceSnippet());
        match.setFitTagsJson(writeStringList(fitTags));
        match.setConfirmStatus("pending");
        match.setCreatedAt(now);
        match.setUpdatedAt(now);
        match.setIsDelete(0);
        return match;
    }

    private boolean hasAchievementDocId(AchievementSearchHitVO hit) {
        return hit != null
                && hit.getAchievementDocId() != null
                && !hit.getAchievementDocId().isBlank();
    }

    private DemandMatchVO toMatchVO(DemandMatch match) {
        DemandMatchVO vo = new DemandMatchVO();
        vo.setMatchId(match.getId() == null ? null : String.valueOf(match.getId()));
        vo.setResultId(match.getAchievementDocId());
        vo.setResultTitle(match.getResultTitle());
        vo.setResultType(match.getResultType());
        vo.setOwner(match.getOwner());
        vo.setDepartment(match.getDepartment());
        vo.setEsScore(match.getEsScore());
        vo.setRuleScore(match.getRuleScore());
        vo.setLlmScore(match.getLlmScore());
        vo.setMatchScore(match.getMatchScore());
        vo.setReason(match.getReason());
        vo.setSourceSnippet(match.getSourceSnippet());
        vo.setFitTags(readStringList(match.getFitTagsJson()));
        vo.setConfirmStatus(match.getConfirmStatus());
        vo.setUpdatedAt(match.getUpdatedAt() == null ? match.getCreatedAt() : match.getUpdatedAt());
        return vo;
    }

    private DemandFollowUpVO toFollowUpVO(DemandFollowUp followUp) {
        DemandFollowUpVO vo = new DemandFollowUpVO();
        vo.setLabel(followUp.getNextAction());
        vo.setOwner(followUp.getOwnerName());
        vo.setStatus(followUp.getStatus());
        vo.setDueAt(followUp.getDueAt());
        vo.setNote(followUp.getNote());
        return vo;
    }

    private DemandSourceVO toSourceVO(DemandSource source) {
        DemandSourceVO vo = new DemandSourceVO();
        vo.setId(source.getId());
        vo.setName(source.getName());
        vo.setType(source.getType());
        vo.setIndustry(source.getIndustry());
        vo.setRegion(source.getRegion());
        vo.setFrequencyHours(source.getFrequencyHours());
        vo.setEnabled(source.getEnabled());
        vo.setStatus(source.getStatus());
        vo.setLastRunAt(source.getLastRunAt());
        vo.setLastSuccessAt(source.getLastSuccessAt());
        vo.setFailureReason(source.getFailureReason());
        vo.setSuccessRate("healthy".equals(source.getStatus()) ? 100 : 0);
        vo.setNewCount(0);
        vo.setMatchedCount(0);
        return vo;
    }

    private String buildDemandQuery(DemandItem item) {
        return joinText(
                item.getTitle(),
                item.getLlmSummary(),
                item.getSummary(),
                truncate(item.getRawContent(), 4000),
                item.getIndustry(),
                item.getRegion(),
                String.join(" ", readStringList(item.getKeywordsJson())),
                String.join(" ", readStringList(item.getTagsJson())));
    }

    private String buildPreviewQuery(DemandMatchPreviewRequest request) {
        return joinText(
                request.getTitle(),
                request.getContent(),
                request.getIndustry(),
                request.getRegion(),
                request.getKeywords() == null ? null : String.join(" ", request.getKeywords()));
    }

    private List<String> matchedKeywords(List<String> keywords, AchievementSearchHitVO hit) {
        if (keywords == null || keywords.isEmpty()) {
            return List.of();
        }
        String haystack = joinText(
                hit.getTitle(),
                hit.getSummary(),
                hit.getKeywordsText(),
                hit.getSourceSnippet(),
                hit.getAttachmentNames()).toLowerCase(Locale.ROOT);
        LinkedHashSet<String> matched = new LinkedHashSet<>();
        for (String keyword : keywords) {
            if (keyword != null && !keyword.isBlank() && haystack.contains(keyword.toLowerCase(Locale.ROOT))) {
                matched.add(keyword);
            }
        }
        return new ArrayList<>(matched);
    }

    private double calculateRuleScore(DemandItem item, AchievementSearchHitVO hit, List<String> hitKeywords) {
        double score = 0.2D;
        List<String> demandKeywords = readStringList(item.getKeywordsJson());
        if (!demandKeywords.isEmpty()) {
            score += Math.min(0.45D, hitKeywords.size() * 0.12D);
        }
        if (hit.getTitle() != null && item.getTitle() != null && hasCommonText(item.getTitle(), hit.getTitle())) {
            score += 0.15D;
        }
        if (hit.getSourceSnippet() != null && !hit.getSourceSnippet().isBlank()) {
            score += 0.1D;
        }
        if (hit.getAttachmentNames() != null && !hit.getAttachmentNames().isBlank()) {
            score += 0.1D;
        }
        return Math.min(score, 1D);
    }

    private boolean hasCommonText(String left, String right) {
        String normalizedLeft = left.replaceAll("\\s+", "");
        String normalizedRight = right.replaceAll("\\s+", "");
        if (normalizedLeft.length() < 4 || normalizedRight.length() < 4) {
            return false;
        }
        for (int i = 0; i <= normalizedLeft.length() - 4; i++) {
            if (normalizedRight.contains(normalizedLeft.substring(i, i + 4))) {
                return true;
            }
        }
        return false;
    }

    private List<String> buildFitTags(AchievementSearchHitVO hit, List<String> hitKeywords) {
        List<String> tags = new ArrayList<>();
        tags.add("ES召回");
        if (!hitKeywords.isEmpty()) {
            tags.add("关键词命中");
        }
        if (hit.getSourceSnippet() != null && !hit.getSourceSnippet().isBlank()) {
            tags.add("证据片段");
        }
        if (hit.getAttachmentNames() != null && !hit.getAttachmentNames().isBlank()) {
            tags.add("含附件预览");
        }
        if (hit.getTypeName() != null && !hit.getTypeName().isBlank()) {
            tags.add(hit.getTypeName());
        }
        return tags;
    }

    private String buildReason(List<String> hitKeywords, List<String> fitTags) {
        String keywordPart = hitKeywords.isEmpty()
                ? "暂未形成明确关键词交集"
                : "命中关键词「" + String.join("、", hitKeywords) + "」";
        return "ES 检索召回该成果，" + keywordPart + "；匹配标签：" + String.join("、", fitTags) + "。建议人工确认场景、数据和转化边界。";
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(
                    json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            log.debug("JSON数组解析失败，value={}", json, e);
            return List.of();
        }
    }

    private String writeStringList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private String joinText(String... parts) {
        StringJoiner joiner = new StringJoiner("\n");
        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                joiner.add(part.trim());
            }
        }
        return joiner.toString();
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    private BigDecimal decimal(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP);
    }

    private String priorityLabel(String priority) {
        if (priority == null || priority.isBlank()) {
            return "中";
        }
        return switch (priority.trim().toLowerCase(Locale.ROOT)) {
            case "high", "高" -> "高";
            case "low", "低" -> "低";
            default -> "中";
        };
    }

    private String valueLevel(BigDecimal bestMatchScore) {
        double score = bestMatchScore == null ? 0D : bestMatchScore.doubleValue();
        if (score >= 0.8D) {
            return "高价值线索";
        }
        if (score >= 0.6D) {
            return "重点跟踪";
        }
        return "待研判";
    }

    private String resolveUserName(KeycloakUser user) {
        if (user == null) {
            return null;
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return user.getName();
    }
}
