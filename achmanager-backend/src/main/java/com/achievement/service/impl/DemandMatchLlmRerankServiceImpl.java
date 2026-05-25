package com.achievement.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.achievement.client.LlmClient;
import com.achievement.config.RagProperties;
import com.achievement.constant.LlmUsage;
import com.achievement.domain.ChatMessage;
import com.achievement.domain.po.DemandItem;
import com.achievement.domain.po.DemandMatch;
import com.achievement.service.IDemandMatchLlmRerankService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemandMatchLlmRerankServiceImpl implements IDemandMatchLlmRerankService {

    private final LlmClient llmClient;
    private final RagProperties ragProperties;
    private final ObjectMapper objectMapper;

    @Override
    public List<DemandMatch> rerank(DemandItem demand, List<DemandMatch> candidates, double maxEsScore) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        if (!ragProperties.getLlm().isEnabled()) {
            return sortByMatchScore(candidates);
        }

        int limit = Math.min(
                Math.max(ragProperties.getLlm().getRerankMaxCandidates(), 1),
                candidates.size());
        List<DemandMatch> rerankScope = new ArrayList<>(candidates.subList(0, limit));
        try {
            Map<String, LlmDecision> decisions = callLlm(demand, rerankScope);
            for (DemandMatch match : rerankScope) {
                LlmDecision decision = decisions.get(match.getAchievementDocId());
                if (decision == null) {
                    continue;
                }
                applyDecision(match, decision, maxEsScore);
            }
        } catch (Exception e) {
            log.warn("需求-成果 LLM 重排失败，已回退到 ES+规则匹配，demandId={}, 错误={}",
                    demand == null ? null : demand.getId(), e.getMessage());
        }

        List<DemandMatch> merged = new ArrayList<>(rerankScope);
        if (candidates.size() > limit) {
            merged.addAll(candidates.subList(limit, candidates.size()));
        }
        return sortByMatchScore(merged);
    }

    private Map<String, LlmDecision> callLlm(DemandItem demand, List<DemandMatch> candidates) throws JsonProcessingException {
        String responseJson = llmClient.chatCompletion(
                LlmUsage.RAG,
                List.of(
                        ChatMessage.system(buildSystemPrompt()),
                        ChatMessage.user(buildUserPrompt(demand, candidates))),
                ragProperties.getLlm().getTemperature(),
                ragProperties.getLlm().getMaxTokens());

        String content = objectMapper.readTree(responseJson).at("/choices/0/message/content").asText();
        JsonNode root = objectMapper.readTree(stripCodeBlockMarkers(content));
        JsonNode matchesNode = root.isArray() ? root : root.path("matches");

        Map<String, LlmDecision> decisions = new LinkedHashMap<>();
        if (!matchesNode.isArray()) {
            return decisions;
        }
        for (JsonNode node : matchesNode) {
            String achievementDocId = node.path("achievementDocId").asText(null);
            if (achievementDocId == null || achievementDocId.isBlank()) {
                achievementDocId = node.path("resultId").asText(null);
            }
            if (achievementDocId == null || achievementDocId.isBlank()) {
                continue;
            }
            double score = node.has("llmScore")
                    ? node.path("llmScore").asDouble(0D)
                    : node.path("score").asDouble(0D);
            decisions.put(achievementDocId, new LlmDecision(
                    clamp(score),
                    node.path("reason").asText(null),
                    readStringArray(node.path("fitTags")),
                    node.path("sourceSnippet").asText(null)));
        }
        return decisions;
    }

    private String buildSystemPrompt() {
        return """
                你是科研成果转化场景中的需求-成果匹配专家。请根据需求信息和候选成果信息，对候选成果进行重排。

                判断重点：
                1. 需求的真实技术问题、应用场景、行业和交付边界。
                2. 成果的研究内容、关键词、摘要、附件证据片段是否能支撑需求。
                3. 只根据输入材料判断，不要编造不存在的成果能力。
                4. 分数 0-1，0.80 以上表示高度匹配，0.60-0.79 表示可跟进确认，0.60 以下表示弱匹配。

                只返回 JSON，不要返回 Markdown、解释性段落或代码块。格式如下：
                {
                  "matches": [
                    {
                      "achievementDocId": "候选成果ID",
                      "llmScore": 0.82,
                      "reason": "一句到两句话说明为什么匹配或不匹配",
                      "sourceSnippet": "最关键的证据片段，可为空",
                      "fitTags": ["技术方向一致", "需确认数据条件"]
                    }
                  ]
                }
                """;
    }

    private String buildUserPrompt(DemandItem demand, List<DemandMatch> candidates) throws JsonProcessingException {
        Map<String, Object> payload = new LinkedHashMap<>();
        Map<String, Object> demandPayload = new LinkedHashMap<>();
        demandPayload.put("id", demand == null ? null : demand.getId());
        demandPayload.put("title", demand == null ? null : demand.getTitle());
        demandPayload.put("industry", demand == null ? null : demand.getIndustry());
        demandPayload.put("region", demand == null ? null : demand.getRegion());
        demandPayload.put("summary", demand == null ? null : demand.getSummary());
        demandPayload.put("llmSummary", demand == null ? null : demand.getLlmSummary());
        demandPayload.put("rawContentPreview", demand == null ? null : truncate(demand.getRawContent(), 2500));
        demandPayload.put("keywordsJson", demand == null ? null : demand.getKeywordsJson());
        demandPayload.put("tagsJson", demand == null ? null : demand.getTagsJson());
        payload.put("demand", demandPayload);

        List<Map<String, Object>> candidatePayload = new ArrayList<>();
        for (DemandMatch candidate : candidates) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("achievementDocId", candidate.getAchievementDocId());
            item.put("title", candidate.getResultTitle());
            item.put("type", candidate.getResultType());
            item.put("owner", candidate.getOwner());
            item.put("department", candidate.getDepartment());
            item.put("esScore", candidate.getEsScore());
            item.put("ruleScore", candidate.getRuleScore());
            item.put("currentReason", candidate.getReason());
            item.put("sourceSnippet", truncate(candidate.getSourceSnippet(), 600));
            item.put("fitTagsJson", candidate.getFitTagsJson());
            candidatePayload.add(item);
        }
        payload.put("candidates", candidatePayload);

        return "请对以下候选成果按需求匹配度评分，并返回严格 JSON：\n"
                + objectMapper.writeValueAsString(payload);
    }

    private void applyDecision(DemandMatch match, LlmDecision decision, double maxEsScore) {
        match.setLlmScore(decimal(decision.score(), 3));
        if (decision.reason() != null && !decision.reason().isBlank()) {
            match.setReason(decision.reason());
        }
        if (decision.sourceSnippet() != null && !decision.sourceSnippet().isBlank()) {
            match.setSourceSnippet(decision.sourceSnippet());
        }
        match.setFitTagsJson(writeStringList(mergeTags(readStringArray(match.getFitTagsJson()), decision.fitTags())));

        double esNormalized = match.getEsScore() == null || maxEsScore <= 0
                ? 0D
                : match.getEsScore().doubleValue() / maxEsScore;
        double ruleScore = match.getRuleScore() == null ? 0D : match.getRuleScore().doubleValue();
        double finalScore = esNormalized * 0.5D + ruleScore * 0.2D + decision.score() * 0.3D;
        match.setMatchScore(decimal(Math.min(finalScore, 0.99D), 3));
    }

    private List<DemandMatch> sortByMatchScore(List<DemandMatch> matches) {
        return matches.stream()
                .sorted(Comparator.comparing(
                        (DemandMatch match) -> match.getMatchScore() == null ? BigDecimal.ZERO : match.getMatchScore())
                        .reversed())
                .toList();
    }

    private List<String> readStringArray(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            String value = item.asText(null);
            if (value != null && !value.isBlank()) {
                values.add(value);
            }
        }
        return values;
    }

    private List<String> readStringArray(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(
                    json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private List<String> mergeTags(List<String> existing, List<String> incoming) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        if (existing != null) {
            tags.addAll(existing);
        }
        if (incoming != null) {
            tags.addAll(incoming);
        }
        return new ArrayList<>(tags);
    }

    private String writeStringList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private String stripCodeBlockMarkers(String content) {
        if (content == null) {
            return "{}";
        }
        String text = content.trim();
        if (text.startsWith("```")) {
            int firstLineEnd = text.indexOf('\n');
            if (firstLineEnd >= 0) {
                text = text.substring(firstLineEnd + 1).trim();
            }
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3).trim();
        }
        return text;
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    private double clamp(double score) {
        if (score < 0D) {
            return 0D;
        }
        if (score > 1D) {
            return 1D;
        }
        return score;
    }

    private BigDecimal decimal(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP);
    }

    private record LlmDecision(double score, String reason, List<String> fitTags, String sourceSnippet) {
    }
}
