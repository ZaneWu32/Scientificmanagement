package com.achievement.service.impl;

import com.achievement.client.CrawlerClient;
import com.achievement.config.CrawlerProperties;
import com.achievement.domain.dto.CrawlerResultDTO;
import com.achievement.domain.po.CrawlerPolicy;
import com.achievement.domain.po.CrawlerPolicyAchievementMatch;
import com.achievement.domain.vo.CrawlerStatusVO;
import com.achievement.domain.vo.PolicyVO;
import com.achievement.mapper.CrawlerPolicyMapper;
import com.achievement.mapper.CrawlerPolicyMatchMapper;
import com.achievement.service.ICrawlerPolicyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlerPolicyServiceImpl implements ICrawlerPolicyService {

    private final CrawlerClient crawlerClient;
    private final CrawlerProperties crawlerProperties;
    private final CrawlerPolicyMapper crawlerPolicyMapper;
    private final CrawlerPolicyMatchMapper crawlerPolicyMatchMapper;
    private final ObjectMapper objectMapper;
    private final SqlSessionFactory sqlSessionFactory;

    private static final double MATCH_THRESHOLD = 0.1;
    private final ConcurrentHashMap<String, String> syncStatus = new ConcurrentHashMap<>();
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
            syncStatus.put(crawlerId, "syncing");
        }
        int totalNew = 0;
        for (String crawlerId : crawlerNames.keySet()) {
            try {
                totalNew += syncCrawlerInternal(crawlerId);
                syncStatus.put(crawlerId, "completed");
            } catch (Exception e) {
                syncStatus.put(crawlerId, "failed");
                log.error("同步爬虫 {} 失败: {}", crawlerId, e.getMessage(), e);
            }
        }
        log.info("爬虫数据同步完成，新增 {} 条", totalNew);
    }

    @Override
    public void syncCrawler(String crawlerId) {
        syncStatus.put(crawlerId, "syncing");
        try {
            int newCount = syncCrawlerInternal(crawlerId);
            syncStatus.put(crawlerId, "completed");
            log.info("爬虫 {} 同步完成，新增 {} 条", crawlerId, newCount);
        } catch (Exception e) {
            syncStatus.put(crawlerId, "failed");
            log.error("同步爬虫 {} 失败: {}", crawlerId, e.getMessage(), e);
            throw new RuntimeException("同步爬虫 " + crawlerId + " 失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void triggerCrawlerSync(String crawlerId) {
        syncStatus.put(crawlerId, "syncing");
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
            syncStatus.put(id, "syncing");
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
    public Map<String, String> getCrawlerNames() {
        return crawlerClient.listCrawlers();
    }

    @Override
    public List<CrawlerStatusVO> getCrawlerStatusList() {
        Map<String, String> names = crawlerClient.listCrawlers();
        Map<String, String> taskStatuses = crawlerClient.getAllStatuses();

        // 查询每个爬虫的政策数量
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
            vo.setSyncStatus(syncStatus.getOrDefault(id, "idle"));
            vo.setCrawlerStatus(taskStatuses.getOrDefault(id, "idle"));
            vo.setPolicyCount(countMap.getOrDefault(id, 0L));
            result.add(vo);
        }
        return result;
    }

    private int syncCrawlerInternal(String crawlerId) {
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
                continue;
            }

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
            policy.setKeywordsExtracted("[]");
            policy.setDelFlag("0");
            crawlerPolicyMapper.insert(policy);
            newCount++;
        }

        return newCount;
    }

    @Override
    public void matchPoliciesWithAchievements() {
        log.info("开始政策-成果物匹配");

        Page<CrawlerPolicy> page = new Page<>(1, 100);
        LambdaQueryWrapper<CrawlerPolicy> query = new LambdaQueryWrapper<>();
        query.eq(CrawlerPolicy::getDelFlag, "0");
        IPage<CrawlerPolicy> policyPage = crawlerPolicyMapper.selectPage(page, query);

        int matchCount = 0;
        for (CrawlerPolicy policy : policyPage.getRecords()) {
            List<String> policyKeywords = extractKeywords(policy.getTitle() + " " + policy.getContent());
            if (policyKeywords.isEmpty()) {
                continue;
            }

            try (SqlSession session = sqlSessionFactory.openSession()) {
                var conn = session.getConnection();
                var stmt = conn.prepareStatement(
                        "SELECT document_id, title, keywords FROM achievement_mains WHERE is_delete = 0");
                var rs = stmt.executeQuery();

                while (rs.next()) {
                    String docId = rs.getString("document_id");
                    String achTitle = rs.getString("title");
                    String achKeywordsJson = rs.getString("keywords");

                    List<String> achKeywords = parseKeywords(achKeywordsJson);
                    Set<String> achTokens = new HashSet<>(achKeywords);
                    achTokens.addAll(extractKeywords(achTitle));

                    double score = jaccardSimilarity(new HashSet<>(policyKeywords), achTokens);
                    if (score < MATCH_THRESHOLD) {
                        continue;
                    }

                    LambdaQueryWrapper<CrawlerPolicyAchievementMatch> existsQuery = new LambdaQueryWrapper<>();
                    existsQuery.eq(CrawlerPolicyAchievementMatch::getPolicyId, policy.getId())
                            .eq(CrawlerPolicyAchievementMatch::getAchievementDocumentId, docId)
                            .eq(CrawlerPolicyAchievementMatch::getMatchMethod, "keyword");
                    if (crawlerPolicyMatchMapper.selectCount(existsQuery) > 0) {
                        continue;
                    }

                    CrawlerPolicyAchievementMatch match = new CrawlerPolicyAchievementMatch();
                    match.setPolicyId(policy.getId());
                    match.setAchievementDocumentId(docId);
                    match.setMatchScore(BigDecimal.valueOf(score).setScale(4, BigDecimal.ROUND_HALF_UP));
                    match.setMatchMethod("keyword");
                    match.setMatchReason("关键词重叠度: " + String.format("%.1f%%", score * 100));
                    match.setIsActive(1);
                    crawlerPolicyMatchMapper.insert(match);
                    matchCount++;
                }

                rs.close();
                stmt.close();
            } catch (Exception e) {
                log.error("匹配政策 {} 时出错: {}", policy.getId(), e.getMessage(), e);
            }
        }

        log.info("政策-成果物匹配完成，新增 {} 条匹配", matchCount);
    }

    @Override
    public List<PolicyVO> getRelatedPolicies(String achievementDocId, int limit) {
        return crawlerPolicyMapper.selectRelatedPolicies(achievementDocId, limit);
    }

    @Override
    public IPage<PolicyVO> getAllPolicies(int page, int pageSize) {
        Page<CrawlerPolicy> policyPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<CrawlerPolicy> query = new LambdaQueryWrapper<>();
        query.eq(CrawlerPolicy::getDelFlag, "0")
             .orderByDesc(CrawlerPolicy::getCreateTime);
        IPage<CrawlerPolicy> result = crawlerPolicyMapper.selectPage(policyPage, query);

        Page<PolicyVO> voPage = new Page<>(page, pageSize, result.getTotal());
        List<PolicyVO> voList = result.getRecords().stream().map(p -> {
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
        }).collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    @Scheduled(cron = "${crawler.sync-cron}")
    public void scheduledSync() {
        log.info("定时任务: 开始爬虫数据同步");
        try {
            syncAllCrawlers();
            matchPoliciesWithAchievements();
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
        List<String> keywords = new ArrayList<>();
        for (String token : tokens) {
            String t = token.trim().toLowerCase();
            if (t.length() >= 2 && !stopwords.contains(t)) {
                keywords.add(t);
            }
        }
        return keywords.stream().distinct().limit(50).collect(Collectors.toList());
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
