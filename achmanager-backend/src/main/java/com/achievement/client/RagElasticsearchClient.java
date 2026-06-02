package com.achievement.client;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.achievement.config.RagProperties;
import com.achievement.domain.po.AchievementSearchDoc;
import com.achievement.domain.vo.AchievementSearchHitVO;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RagElasticsearchClient {

    private static final DateTimeFormatter ES_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Qualifier("ragElasticsearchRestClient")
    private final RestClient restClient;
    private final RagProperties ragProperties;

    public Map<String, Object> health() {
        ensureEnabled();
        JsonNode root = restClient.get()
                .uri("/")
                .retrieve()
                .body(JsonNode.class);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enabled", true);
        result.put("baseUrl", ragProperties.getElasticsearch().getBaseUrl());
        result.put("clusterName", root == null ? null : root.path("cluster_name").asText(null));
        result.put("version", root == null ? null : root.path("version").path("number").asText(null));
        return result;
    }

    public void ensureAchievementIndex() {
        ensureEnabled();
        String index = achievementIndex();
        if (indexExists(index)) {
            return;
        }
        restClient.put()
                .uri("/{index}", index)
                .body(achievementIndexBody())
                .retrieve()
                .toBodilessEntity();
    }

    public void indexAchievement(AchievementSearchDoc doc) {
        ensureEnabled();
        if (doc == null || doc.getAchievementDocId() == null || doc.getAchievementDocId().isBlank()) {
            throw new IllegalArgumentException("achievementDocId不能为空");
        }
        ensureAchievementIndex();
        restClient.put()
                .uri("/{index}/_doc/{id}", achievementIndex(), doc.getAchievementDocId())
                .body(toEsDocument(doc))
                .retrieve()
                .toBodilessEntity();
    }

    public void deleteAchievement(String achievementDocId) {
        ensureEnabled();
        if (achievementDocId == null || achievementDocId.isBlank()) {
            return;
        }
        try {
            restClient.delete()
                    .uri("/{index}/_doc/{id}", achievementIndex(), achievementDocId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() != HttpStatus.NOT_FOUND) {
                throw e;
            }
        }
    }

    public List<AchievementSearchHitVO> searchAchievements(String keyword, int topK) {
        ensureEnabled();
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        ensureAchievementIndex();
        int size = topK <= 0 ? ragProperties.getElasticsearch().getSearchTopK() : Math.min(topK, 50);
        JsonNode root = restClient.post()
                .uri("/{index}/_search", achievementIndex())
                .body(searchBody(keyword, size))
                .retrieve()
                .body(JsonNode.class);

        List<AchievementSearchHitVO> hits = new ArrayList<>();
        JsonNode hitArray = root == null ? null : root.path("hits").path("hits");
        if (hitArray == null || !hitArray.isArray()) {
            return hits;
        }
        for (JsonNode hit : hitArray) {
            JsonNode source = hit.path("_source");
            AchievementSearchHitVO vo = new AchievementSearchHitVO();
            vo.setAchievementDocId(text(source, "achievementDocId"));
            vo.setTitle(text(source, "title"));
            vo.setTypeName(text(source, "typeName"));
            vo.setTypeCode(text(source, "typeCode"));
            vo.setSummary(text(source, "summary"));
            vo.setKeywordsText(text(source, "keywordsText"));
            vo.setAuthorsText(text(source, "authorsText"));
            vo.setProjectName(text(source, "projectName"));
            vo.setYear(text(source, "year"));
            vo.setVisibilityRange(text(source, "visibilityRange"));
            vo.setAttachmentNames(text(source, "attachmentNames"));
            vo.setSourceSnippet(extractSnippet(hit, source));
            vo.setEsScore(hit.path("_score").isNumber() ? hit.path("_score").asDouble() : 0D);
            hits.add(vo);
        }
        return hits;
    }

    private boolean indexExists(String index) {
        try {
            restClient.head()
                    .uri("/{index}", index)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return false;
            }
            throw e;
        } catch (RestClientException e) {
            throw e;
        }
    }

    private String achievementIndex() {
        return ragProperties.getElasticsearch().getAchievementIndex();
    }

    private Map<String, Object> achievementIndexBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("number_of_shards", 1);
        settings.put("number_of_replicas", 0);
        body.put("settings", settings);

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("achievementDocId", field("keyword"));
        properties.put("title", textWithKeywordField());
        properties.put("typeName", textWithKeywordField());
        properties.put("typeCode", field("keyword"));
        properties.put("summary", field("text"));
        properties.put("keywordsText", field("text"));
        properties.put("authorsText", field("text"));
        properties.put("projectName", textWithKeywordField());
        properties.put("year", field("keyword"));
        properties.put("visibilityRange", field("keyword"));
        properties.put("attachmentNames", field("text"));
        properties.put("attachmentTextPreview", field("text"));
        properties.put("searchText", field("text"));
        properties.put("searchHash", field("keyword"));
        properties.put("indexedAt", field("date"));
        properties.put("isDelete", field("integer"));

        Map<String, Object> mappings = new LinkedHashMap<>();
        mappings.put("properties", properties);
        body.put("mappings", mappings);
        return body;
    }

    private Map<String, Object> field(String type) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("type", type);
        return field;
    }

    private Map<String, Object> textWithKeywordField() {
        Map<String, Object> field = field("text");
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("keyword", field("keyword"));
        field.put("fields", fields);
        return field;
    }

    private Map<String, Object> toEsDocument(AchievementSearchDoc doc) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("achievementDocId", doc.getAchievementDocId());
        putIfNotBlank(body, "title", doc.getTitle());
        putIfNotBlank(body, "typeName", doc.getTypeName());
        putIfNotBlank(body, "typeCode", doc.getTypeCode());
        putIfNotBlank(body, "summary", doc.getSummary());
        putIfNotBlank(body, "keywordsText", doc.getKeywordsText());
        putIfNotBlank(body, "authorsText", doc.getAuthorsText());
        putIfNotBlank(body, "projectName", doc.getProjectName());
        putIfNotBlank(body, "year", doc.getYear());
        putIfNotBlank(body, "visibilityRange", doc.getVisibilityRange());
        putIfNotBlank(body, "attachmentNames", doc.getAttachmentNames());
        putIfNotBlank(body, "attachmentTextPreview", doc.getAttachmentTextPreview());
        putIfNotBlank(body, "searchText", doc.getSearchText());
        putIfNotBlank(body, "searchHash", doc.getSearchHash());
        body.put("indexedAt", format(doc.getIndexedAt()));
        body.put("isDelete", doc.getIsDelete() == null ? 0 : doc.getIsDelete());
        return body;
    }

    private Map<String, Object> searchBody(String keyword, int size) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("size", size);
        body.put("_source", List.of(
                "achievementDocId",
                "title",
                "typeName",
                "typeCode",
                "summary",
                "keywordsText",
                "authorsText",
                "projectName",
                "year",
                "visibilityRange",
                "attachmentNames",
                "searchText"));

        Map<String, Object> multiMatch = new LinkedHashMap<>();
        multiMatch.put("query", keyword);
        multiMatch.put("fields", List.of(
                "title^4",
                "keywordsText^3",
                "summary^2",
                "typeName^1.5",
                "projectName^1.3",
                "attachmentNames",
                "attachmentTextPreview",
                "searchText^2"));
        multiMatch.put("type", "best_fields");

        Map<String, Object> phrase = new LinkedHashMap<>();
        phrase.put("query", keyword);
        phrase.put("boost", 2);

        Map<String, Object> bool = new LinkedHashMap<>();
        bool.put("filter", List.of(Map.of("term", Map.of("isDelete", 0))));
        bool.put("should", List.of(
                Map.of("multi_match", multiMatch),
                Map.of("match_phrase", Map.of("searchText", phrase))));
        bool.put("minimum_should_match", 1);
        body.put("query", Map.of("bool", bool));

        Map<String, Object> highlightFields = new LinkedHashMap<>();
        highlightFields.put("title", Map.of("number_of_fragments", 0));
        highlightFields.put("summary", Map.of("fragment_size", 180, "number_of_fragments", 1));
        highlightFields.put("searchText", Map.of("fragment_size", 180, "number_of_fragments", 1));
        highlightFields.put("attachmentTextPreview", Map.of("fragment_size", 180, "number_of_fragments", 1));
        Map<String, Object> highlight = new LinkedHashMap<>();
        highlight.put("pre_tags", List.of(""));
        highlight.put("post_tags", List.of(""));
        highlight.put("fields", highlightFields);
        body.put("highlight", highlight);
        return body;
    }

    private String extractSnippet(JsonNode hit, JsonNode source) {
        JsonNode highlight = hit.path("highlight");
        for (String field : List.of("summary", "searchText", "attachmentTextPreview", "title")) {
            JsonNode fragments = highlight.path(field);
            if (fragments.isArray() && fragments.size() > 0) {
                return truncate(fragments.get(0).asText(), 240);
            }
        }
        String summary = text(source, "summary");
        if (summary != null && !summary.isBlank()) {
            return truncate(summary, 240);
        }
        return truncate(text(source, "searchText"), 240);
    }

    private String text(JsonNode node, String field) {
        if (node == null || node.path(field).isMissingNode() || node.path(field).isNull()) {
            return null;
        }
        String value = node.path(field).asText(null);
        return value == null || value.isBlank() ? null : value;
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private void putIfNotBlank(Map<String, Object> target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.put(key, value);
        }
    }

    private String format(LocalDateTime value) {
        return value == null ? null : ES_DATE_FORMATTER.format(value);
    }

    private void ensureEnabled() {
        if (!ragProperties.getElasticsearch().isEnabled()) {
            throw new IllegalStateException("RAG Elasticsearch 未启用");
        }
    }
}
