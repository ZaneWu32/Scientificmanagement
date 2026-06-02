package com.achievement.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.achievement.client.CrawlerClient;
import com.achievement.config.CrawlerProperties;
import com.achievement.domain.dto.AchMainBaseRow;
import com.achievement.domain.dto.CrawlerResultDTO;
import com.achievement.domain.dto.PolicyQueryDTO;
import com.achievement.domain.po.CrawlerPolicy;
import com.achievement.domain.po.DemandItem;
import com.achievement.domain.vo.CrawlerStatusVO;
import com.achievement.domain.vo.PolicyVO;
import com.achievement.mapper.AchievementMainsMapper;
import com.achievement.mapper.CrawlerPolicyMapper;
import com.achievement.mapper.DemandItemMapper;
import com.achievement.service.ICrawlerPolicyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlerPolicyServiceImpl implements ICrawlerPolicyService {

    private final CrawlerClient crawlerClient;
    private final CrawlerProperties crawlerProperties;
    private final CrawlerPolicyMapper crawlerPolicyMapper;
    private final AchievementMainsMapper mainsMapper;
    private final DemandItemMapper demandItemMapper;
    private final ObjectMapper objectMapper;

    private static final double MATCH_THRESHOLD = 0.1;
    private final ConcurrentHashMap<String, String> crawlerState = new ConcurrentHashMap<>();
    private final ExecutorService syncExecutor = Executors.newCachedThreadPool();

    @Override
    public void syncAllCrawlers() {
        Map<String, String> crawlerNames = crawlerClient.listCrawlers();
        if (crawlerNames.isEmpty()) {
            log.warn("未获取到可用爬虫列表，跳过同步");
            return;
        }
        log.info("开始同步爬虫数据，共 {} 个爬虫", crawlerNames.size());
        for (String crawlerId : crawlerNames.keySet()) {
            crawlerState.put(crawlerId, "running");
        }
        int totalNew = 0;
        for (Map.Entry<String, String> entry : crawlerNames.entrySet()) {
            String crawlerId = entry.getKey();
            try {
                totalNew += syncCrawlerInternal(crawlerId, entry.getValue());
                crawlerState.put(crawlerId, "completed");
            } catch (Exception e) {
                crawlerState.put(crawlerId, "failed");
                log.error("同步爬虫 {} 失败: {}", crawlerId, e.getMessage(), e);
            }
        }
        log.info("爬虫数据同步完成，新增 {} 条", totalNew);
    }

    @Override
    public void syncCrawler(String crawlerId) {
        crawlerState.put(crawlerId, "running");
        try {
            String crawlerName = crawlerClient.listCrawlers().getOrDefault(crawlerId, crawlerId);
            int newCount = syncCrawlerInternal(crawlerId, crawlerName);
            crawlerState.put(crawlerId, "completed");
            log.info("爬虫 {} 同步完成，新增 {} 条", crawlerId, newCount);
        } catch (Exception e) {
            crawlerState.put(crawlerId, "failed");
            log.error("同步爬虫 {} 失败: {}", crawlerId, e.getMessage(), e);
            throw new RuntimeException("同步爬虫 " + crawlerId + " 失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void triggerCrawlerSync(String crawlerId) {
        crawlerState.put(crawlerId, "running");
        CompletableFuture.runAsync(() -> {
            try {
                syncCrawler(crawlerId);
            } catch (Exception e) {
                log.error("异步同步爬虫 {} 失败: {}", crawlerId, e.getMessage(), e);
            }
        }, syncExecutor);
    }

    @Override
    public void triggerSyncAll() {
        Map<String, String> names = crawlerClient.listCrawlers();
        for (String id : names.keySet()) {
            crawlerState.put(id, "running");
        }
        CompletableFuture.runAsync(() -> {
            try {
                syncAllCrawlers();
            } catch (Exception e) {
                log.error("异步全量同步失败: {}", e.getMessage(), e);
            }
        }, syncExecutor);
    }

    @Override
    public int backfillPoliciesToDemands() {
        Map<String, String> crawlerNames = crawlerClient.listCrawlers();
        List<CrawlerPolicy> policies = crawlerPolicyMapper.selectList(
                new LambdaQueryWrapper<CrawlerPolicy>()
                        .eq(CrawlerPolicy::getDelFlag, "0")
                        .orderByDesc(CrawlerPolicy::getPublishDateParsed)
                        .orderByDesc(CrawlerPolicy::getCreateTime));
        int before = countDemandItems();
        for (CrawlerPolicy policy : policies) {
            CrawlerResultDTO dto = new CrawlerResultDTO();
            dto.setSource(policy.getSourceUrl());
            dto.setTitle(policy.getTitle());
            dto.setDatetime(policy.getPublishDate());
            dto.setContent(policy.getContent());
            dto.setHrefs(parseKeywords(policy.getHrefs()));
            ensureDemandItemFromCrawlerResult(
                    policy.getCrawlerId(),
                    crawlerNames.getOrDefault(policy.getCrawlerId(), policy.getCrawlerId()),
                    dto,
                    policy.getContentHash(),
                    parseKeywords(policy.getKeywordsExtracted()));
        }
        int inserted = countDemandItems() - before;
        log.info("已从 crawler_policies 补写需求线索 {} 条", inserted);
        return Math.max(inserted, 0);
    }

    @Override
    public Map<String, String> getCrawlerNames() {
        return crawlerClient.listCrawlers();
    }

    @Override
    public List<CrawlerStatusVO> getCrawlerStatusList() {
        Map<String, String> names = crawlerClient.listCrawlers();
        Map<String, CrawlerStatusVO> upstream = crawlerClient.getAllStatuses();

        Map<String, Long> countMap = new HashMap<>();
        for (Map<String, Object> row : crawlerPolicyMapper.countGroupByCrawlerId()) {
            String cid = (String) row.get("crawler_id");
            Number cnt = (Number) row.get("cnt");
            if (cid != null && cnt != null) {
                countMap.put(cid, cnt.longValue());
            }
        }

        List<CrawlerStatusVO> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : names.entrySet()) {
            String id = entry.getKey();
            CrawlerStatusVO vo = new CrawlerStatusVO();
            vo.setId(id);
            vo.setName(entry.getValue());
            vo.setStatus(crawlerState.getOrDefault(id, "idle"));
            vo.setPolicyCount(countMap.getOrDefault(id, 0L));
            CrawlerStatusVO upVo = upstream.get(id);
            if (upVo != null && upVo.getStats() != null) {
                vo.setStats(upVo.getStats());
            }
            result.add(vo);
        }
        return result;
    }

    private int syncCrawlerInternal(String crawlerId, String crawlerName) {
        if (!crawlerClient.startCrawl(crawlerId)) {
            log.warn("爬虫 {} 不可用，跳过", crawlerId);
            return 0;
        }

        int attempts = 0;
        int maxAttempts = crawlerProperties.getMaxPollAttempts();
        int pollInterval = crawlerProperties.getPollIntervalMs();

        while (attempts < maxAttempts) {
            String status = crawlerClient.getStatus(crawlerId);
            if ("completed".equals(status)) {
                break;
            } else if ("failed".equals(status)) {
                log.warn("爬虫 {} 执行失败", crawlerId);
                return 0;
            } else if ("unavailable".equals(status)) {
                log.warn("爬虫 {} 服务不可用，跳过", crawlerId);
                return 0;
            }
            try {
                Thread.sleep(pollInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return 0;
            }
            attempts++;
        }

        if (attempts >= maxAttempts) {
            log.warn("爬虫 {} 轮询超时", crawlerId);
            return 0;
        }

        List<CrawlerResultDTO> results = crawlerClient.getResults(crawlerId);
        int newCount = 0;

        for (CrawlerResultDTO dto : results) {
            String hash = computeHash(dto.getTitle(), dto.getSource(), dto.getDatetime());

            LambdaQueryWrapper<CrawlerPolicy> existsQuery = new LambdaQueryWrapper<>();
            existsQuery.eq(CrawlerPolicy::getContentHash, hash);
            if (crawlerPolicyMapper.selectCount(existsQuery) > 0) {
                ensureDemandItemFromCrawlerResult(crawlerId, crawlerName, dto, hash, null);
                continue;
            }

            List<String> keywords = extractKeywords(dto.getTitle() + " " + dto.getContent());
            CrawlerPolicy policy = new CrawlerPolicy();
            policy.setCrawlerId(crawlerId);
            policy.setSourceUrl(dto.getSource());
            policy.setTitle(dto.getTitle());
            policy.setPublishDate(dto.getDatetime());
            policy.setPublishDateParsed(parseDate(dto.getDatetime()));
            policy.setContent(dto.getContent());
            policy.setContentHash(hash);
            try {
                policy.setHrefs(objectMapper.writeValueAsString(dto.getHrefs()));
            } catch (JsonProcessingException e) {
                policy.setHrefs("[]");
            }
            try {
                policy.setKeywordsExtracted(objectMapper.writeValueAsString(
                        keywords));
            } catch (JsonProcessingException e) {
                policy.setKeywordsExtracted("[]");
            }
            policy.setDelFlag("0");
            try {
                crawlerPolicyMapper.insert(policy);
                newCount++;
            } catch (DuplicateKeyException e) {
                // content_hash 已存在，跳过重复数据
            }
            crawlerPolicyMapper.insert(policy);
            ensureDemandItemFromCrawlerResult(crawlerId, crawlerName, dto, hash, keywords);
            newCount++;
        }

        return newCount;
    }

    private void ensureDemandItemFromCrawlerResult(String crawlerId,
                                                   String crawlerName,
                                                   CrawlerResultDTO dto,
                                                   String hash,
                                                   List<String> keywords) {
        if (dto == null || hash == null || hash.isBlank()) {
            return;
        }
        LambdaQueryWrapper<DemandItem> existsQuery = new LambdaQueryWrapper<>();
        existsQuery.eq(DemandItem::getContentHash, hash)
                .last("LIMIT 1");
        if (demandItemMapper.selectCount(existsQuery) > 0) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<String> safeKeywords = keywords == null || keywords.isEmpty()
                ? extractKeywords(dto.getTitle() + " " + dto.getContent())
                : keywords;
        DemandItem item = new DemandItem();
        item.setDemandCode(buildDemandCode(crawlerId, hash));
        item.setTitle(blankToDefault(dto.getTitle(), "未命名外部线索"));
        item.setRawContent(dto.getContent());
        item.setSummary(truncate(dto.getContent(), 500));
        item.setLlmSummary(truncate(dto.getContent(), 500));
        item.setKeywordsJson(writeStringList(safeKeywords.stream().limit(20).toList()));
        item.setTagsJson(writeStringList(List.of("爬虫导入", resolveSourceCategory(crawlerId), crawlerId)));
        item.setIndustry(resolveIndustry(crawlerId, dto));
        item.setRegion(resolveRegion(crawlerId));
        item.setSourceCategory(resolveSourceCategory(crawlerId));
        item.setSourceSite(blankToDefault(crawlerName, crawlerId));
        item.setSourceUrl(dto.getSource());
        item.setCapturedAt(now);
        item.setPriority(resolvePriority(crawlerId));
        item.setConfidence(BigDecimal.valueOf(0.650));
        item.setBestMatchScore(BigDecimal.ZERO);
        item.setStatus("new");
        item.setPendingConfirmationsJson(writeStringList(List.of("请确认该公告是否属于可跟进的真实需求线索")));
        item.setRiskNotesJson(writeStringList(List.of("来源为公开网页采集，需人工确认有效性和跟进边界")));
        item.setContentHash(hash);
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        item.setIsDelete(0);
        demandItemMapper.insert(item);
    }

    private String buildDemandCode(String crawlerId, String hash) {
        String safeCrawlerId = crawlerId == null || crawlerId.isBlank()
                ? "CRAWLER"
                : crawlerId.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]", "_");
        String suffix = hash == null || hash.length() < 12 ? String.valueOf(System.currentTimeMillis()) : hash.substring(0, 12);
        return "CRAWLER-" + safeCrawlerId + "-" + suffix;
    }

    private String resolveSourceCategory(String crawlerId) {
        return switch (crawlerId) {
            case "c2", "c6" -> "招投标公告";
            case "c1", "c3_1", "c3_2" -> "科技项目公告";
            case "c4" -> "产业政策公告";
            case "c5" -> "地方政府动态";
            default -> "外部公告";
        };
    }

    private String resolveIndustry(String crawlerId, CrawlerResultDTO dto) {
        if ("c4".equals(crawlerId)) {
            return "工业和信息化";
        }
        if ("c2".equals(crawlerId) || "c6".equals(crawlerId)) {
            return "公共资源交易";
        }
        String text = (dto == null ? "" : blankToDefault(dto.getTitle(), "") + " " + blankToDefault(dto.getContent(), ""));
        if (text.contains("科技") || text.contains("科研") || text.contains("基金")) {
            return "科技创新";
        }
        return "综合";
    }

    private String resolveRegion(String crawlerId) {
        return switch (crawlerId) {
            case "c4", "c6" -> "嘉兴";
            case "c5" -> "桐乡";
            default -> "浙江";
        };
    }

    private String resolvePriority(String crawlerId) {
        return switch (crawlerId) {
            case "c2", "c6" -> "high";
            case "c4" -> "medium";
            default -> "medium";
        };
    }

    private String writeStringList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        String value = text.trim();
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private int countDemandItems() {
        return Math.toIntExact(demandItemMapper.selectCount(
                new LambdaQueryWrapper<DemandItem>().eq(DemandItem::getIsDelete, 0)));
    }

    @Override
    public List<PolicyVO> matchForAchievement(String achievementDocId, int limit) {
        // 1. 获取成果物关键词
        Set<String> achTokens = getAchievementKeywords(achievementDocId);
        if (achTokens.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 数据库端预过滤：JSON_OVERLAPS 筛选关键词有交集的候选政策
        String keywordsJson;
        try {
            keywordsJson = objectMapper.writeValueAsString(achTokens);
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> candidates = crawlerPolicyMapper.selectFilteredCandidates(keywordsJson, 500);

        // 3. 对候选集计算 Jaccard 相似度
        List<ScoredPolicy> scored = new ArrayList<>();
        for (Map<String, Object> row : candidates) {
            List<String> policyKeywords = parseKeywords((String) row.get("keywords_extracted"));
            if (policyKeywords.isEmpty()) {
                continue;
            }
            double score = jaccardSimilarity(new HashSet<>(policyKeywords), achTokens);
            if (score >= MATCH_THRESHOLD) {
                scored.add(new ScoredPolicy(((Number) row.get("id")).longValue(), score));
            }
        }

        // 4. 取 top N，查详情
        List<Long> topIds = scored.stream()
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .limit(limit)
                .map(sp -> sp.id)
                .collect(Collectors.toList());

        if (topIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, PolicyVO> detailMap = crawlerPolicyMapper.selectPolicyDetailByIds(topIds).stream()
                .collect(Collectors.toMap(PolicyVO::getId, p -> p));

        return scored.stream()
                .filter(sp -> detailMap.containsKey(sp.id))
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .limit(limit)
                .map(sp -> {
                    PolicyVO vo = detailMap.get(sp.id);
                    vo.setMatchScore(BigDecimal.valueOf(sp.score).setScale(4, RoundingMode.HALF_UP));
                    vo.setMatchReason("关键词重叠度: " + String.format("%.1f%%", sp.score * 100));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, List<PolicyVO>> matchForAchievements(List<String> achievementDocIds, int limit) {
        Map<String, List<PolicyVO>> result = new LinkedHashMap<>();
        for (String docId : achievementDocIds) {
            result.put(docId, matchForAchievement(docId, limit));
        }
        return result;
    }

    private Set<String> getAchievementKeywords(String achievementDocId) {
        AchMainBaseRow row = mainsMapper.selectMainBaseByDocId(achievementDocId);
        if (row == null) {
            return Collections.emptySet();
        }
        Set<String> tokens = new HashSet<>();
        tokens.addAll(parseKeywords(row.getKeywordsJson()));
        tokens.addAll(extractKeywords(row.getTitle()));
        return tokens;
    }

    private record ScoredPolicy(long id, double score) {}

    @Override
    public IPage<PolicyVO> getAllPolicies(int page, int pageSize) {
        Page<CrawlerPolicy> policyPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<CrawlerPolicy> query = new LambdaQueryWrapper<>();
        query.eq(CrawlerPolicy::getDelFlag, "0")
             .orderByDesc(CrawlerPolicy::getCreateTime);
        IPage<CrawlerPolicy> result = crawlerPolicyMapper.selectPage(policyPage, query);

        Page<PolicyVO> voPage = new Page<>(page, pageSize, result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toPolicyVO).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public IPage<PolicyVO> queryPolicies(PolicyQueryDTO dto) {
        Page<CrawlerPolicy> page = new Page<>(dto.getPage(), dto.getPageSize());
        LambdaQueryWrapper<CrawlerPolicy> query = new LambdaQueryWrapper<>();
        query.eq(CrawlerPolicy::getDelFlag, "0");

        if (dto.getKeyword() != null && !dto.getKeyword().isBlank()) {
            query.like(CrawlerPolicy::getTitle, dto.getKeyword().trim());
        }
        if (dto.getCrawlerId() != null && !dto.getCrawlerId().isBlank()) {
            query.eq(CrawlerPolicy::getCrawlerId, dto.getCrawlerId());
        }
        if (dto.getStartDate() != null && !dto.getStartDate().isBlank()) {
            query.ge(CrawlerPolicy::getPublishDateParsed, LocalDate.parse(dto.getStartDate()));
        }
        if (dto.getEndDate() != null && !dto.getEndDate().isBlank()) {
            query.le(CrawlerPolicy::getPublishDateParsed, LocalDate.parse(dto.getEndDate()));
        }

        query.orderByDesc(CrawlerPolicy::getPublishDateParsed)
             .orderByDesc(CrawlerPolicy::getCreateTime);

        IPage<CrawlerPolicy> result = crawlerPolicyMapper.selectPage(page, query);
        Page<PolicyVO> voPage = new Page<>(dto.getPage(), dto.getPageSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toPolicyVO).collect(Collectors.toList()));
        return voPage;
    }

    private PolicyVO toPolicyVO(CrawlerPolicy p) {
        PolicyVO vo = new PolicyVO();
        vo.setId(p.getId());
        vo.setCrawlerId(p.getCrawlerId());
        vo.setTitle(p.getTitle());
        vo.setPublishDate(p.getPublishDate());
        vo.setSourceUrl(p.getSourceUrl());
        String content = p.getContent();
        vo.setContentPreview(content != null && content.length() > 300
                ? content.substring(0, 300) + "..." : content);
        try {
            vo.setHrefs(objectMapper.readValue(p.getHrefs(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)));
        } catch (Exception e) {
            vo.setHrefs(Collections.emptyList());
        }
        return vo;
    }

    @Scheduled(cron = "${crawler.sync-cron}")
    public void scheduledSync() {
        log.info("定时任务: 开始爬虫数据同步");
        try {
            syncAllCrawlers();
        } catch (Exception e) {
            log.error("定时同步任务失败: {}", e.getMessage(), e);
        }
    }

    private String computeHash(String title, String source, String datetime) {
        String raw = (title != null ? title : "") + (source != null ? source : "") + (datetime != null ? datetime : "");
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        String cleaned = dateStr.replaceAll("[^0-9\\-]", "").trim();
        String[] patterns = {"yyyy-MM-dd", "yyyy/MM/dd", "yyyyMMdd", "yyyy-MM-dd HH:mm:ss", "yyyy年MM月dd日"};
        for (String pattern : patterns) {
            try {
                return LocalDate.parse(cleaned, DateTimeFormatter.ofPattern(pattern));
            } catch (DateTimeParseException ignored) {
            }
        }
        if (cleaned.length() >= 10) {
            try {
                return LocalDate.parse(cleaned.substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private List<String> extractKeywords(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        Set<String> stopwords = Set.of(
                "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "一个",
                "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好",
                "自己", "这", "他", "她", "它", "们", "那", "些", "什么", "怎么", "如何", "可以",
                "但", "而", "或", "及", "与", "等", "之", "其", "中", "为", "以", "于", "对",
                "将", "被", "从", "把", "向", "让", "给", "又", "再", "已", "还", "更", "最",
                "the", "a", "an", "is", "are", "was", "were", "be", "been", "being",
                "have", "has", "had", "do", "does", "did", "will", "would", "could",
                "should", "may", "might", "can", "shall", "of", "in", "to", "for",
                "with", "on", "at", "from", "by", "and", "or", "not", "this", "that"
        );
        String[] tokens = text.split("[\\s,，。、；：！？!?.()（）\\[\\]【】{}\"'·—\\-]+");
        Set<String> keywords = new LinkedHashSet<>();
        for (String token : tokens) {
            String t = token.trim().toLowerCase();
            if (t.length() < 2 || stopwords.contains(t)) {
                continue;
            }
            keywords.add(t);
            // 中文 token 生成 bigram 提高召回率
            if (t.length() > 2 && t.codePoints().allMatch(Character::isIdeographic)) {
                int[] codePoints = t.codePoints().toArray();
                for (int i = 0; i < codePoints.length - 1; i++) {
                    String bigram = new String(codePoints, i, 2);
                    if (!stopwords.contains(bigram)) {
                        keywords.add(bigram);
                    }
                }
            }
        }
        return keywords.stream().limit(80).collect(Collectors.toList());
    }

    private List<String> parseKeywords(String keywordsJson) {
        if (keywordsJson == null || keywordsJson.isBlank() || "null".equals(keywordsJson)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(keywordsJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private double jaccardSimilarity(Set<String> set1, Set<String> set2) {
        if (set1.isEmpty() || set2.isEmpty()) {
            return 0;
        }
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        return (double) intersection.size() / union.size();
    }
}
