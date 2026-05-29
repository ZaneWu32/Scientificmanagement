package com.achievement.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.achievement.client.RagElasticsearchClient;
import com.achievement.domain.po.AchievementSearchDoc;
import com.achievement.domain.vo.AchDetailVO;
import com.achievement.domain.vo.AchFieldVO;
import com.achievement.domain.vo.AchievementSearchHitVO;
import com.achievement.domain.vo.RagIndexResultVO;
import com.achievement.mapper.AchievementSearchDocMapper;
import com.achievement.service.IAchievementMainsService;
import com.achievement.service.IRagAchievementIndexService;
import com.achievement.utils.AttachmentContentExtractor;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagAchievementIndexServiceImpl implements IRagAchievementIndexService {

    private static final Set<String> SUPPORTED_ATTACHMENT_MIMES = Set.of(
            "text/plain",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final IAchievementMainsService achievementMainsService;
    private final AchievementSearchDocMapper achievementSearchDocMapper;
    private final AttachmentContentExtractor attachmentContentExtractor;
    private final RagElasticsearchClient ragElasticsearchClient;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> elasticsearchHealth() {
        return ragElasticsearchClient.health();
    }

    @Override
    public void ensureAchievementIndex() {
        ragElasticsearchClient.ensureAchievementIndex();
    }

    @Override
    @Transactional
    public AchievementSearchDoc rebuildAchievementDoc(String achievementDocId) {
        if (achievementDocId == null || achievementDocId.isBlank()) {
            throw new IllegalArgumentException("achievementDocId不能为空");
        }
        AchDetailVO detail = achievementMainsService.selectDetailForProjectSystem(achievementDocId);
        AchievementSearchDoc doc = buildSearchDoc(detail);
        upsertSearchDoc(doc);
        return doc;
    }

    @Override
    @Transactional
    public RagIndexResultVO rebuildAndIndexAchievementDoc(String achievementDocId) {
        RagIndexResultVO result = new RagIndexResultVO();
        result.setTotal(1);
        try {
            AchievementSearchDoc doc = rebuildAchievementDoc(achievementDocId);
            ragElasticsearchClient.ensureAchievementIndex();
            ragElasticsearchClient.indexAchievement(doc);
            LocalDateTime now = LocalDateTime.now();
            achievementSearchDocMapper.markEsIndexed(doc.getId(), doc.getAchievementDocId(), now);
            result.setSuccess(1);
        } catch (Exception e) {
            log.warn("重建并写入单个成果 ES 索引失败，achievementDocId={}", achievementDocId, e);
            result.setFailed(1);
            result.getFailedIds().add(achievementDocId);
        }
        return result;
    }

    @Override
    public RagIndexResultVO rebuildAllAchievementDocs() {
        List<String> docIds = achievementSearchDocMapper.selectApprovedAchievementDocIds();
        RagIndexResultVO result = new RagIndexResultVO();
        result.setTotal(docIds.size());
        for (String docId : docIds) {
            try {
                rebuildAchievementDoc(docId);
                result.setSuccess(result.getSuccess() + 1);
            } catch (Exception e) {
                log.warn("重建成果检索快照失败，achievementDocId={}", docId, e);
                result.setFailed(result.getFailed() + 1);
                result.getFailedIds().add(docId);
            }
        }
        return result;
    }

    @Override
    public RagIndexResultVO indexPendingAchievementDocs(Integer limit) {
        int actualLimit = limit == null || limit <= 0 ? 200 : Math.min(limit, 1000);
        List<AchievementSearchDoc> docs = achievementSearchDocMapper.selectPendingEsDocs(actualLimit);
        RagIndexResultVO result = new RagIndexResultVO();
        result.setTotal(docs.size());
        ragElasticsearchClient.ensureAchievementIndex();
        for (AchievementSearchDoc doc : docs) {
            try {
                ragElasticsearchClient.indexAchievement(doc);
                LocalDateTime now = LocalDateTime.now();
                achievementSearchDocMapper.markEsIndexed(doc.getId(), doc.getAchievementDocId(), now);
                result.setSuccess(result.getSuccess() + 1);
            } catch (Exception e) {
                log.warn("成果写入 ES 失败，achievementDocId={}", doc.getAchievementDocId(), e);
                result.setFailed(result.getFailed() + 1);
                result.getFailedIds().add(doc.getAchievementDocId());
            }
        }
        return result;
    }

    @Override
    public RagIndexResultVO rebuildAndIndexAllAchievementDocs() {
        RagIndexResultVO rebuild = rebuildAllAchievementDocs();
        RagIndexResultVO index = indexPendingAchievementDocs(Math.max(rebuild.getSuccess(), 200));
        RagIndexResultVO result = new RagIndexResultVO();
        result.setTotal(rebuild.getTotal());
        result.setSuccess(index.getSuccess());
        result.setFailed(rebuild.getFailed() + index.getFailed());
        result.getFailedIds().addAll(rebuild.getFailedIds());
        result.getFailedIds().addAll(index.getFailedIds());
        return result;
    }

    @Override
    public List<AchievementSearchHitVO> searchAchievements(String keyword, Integer topK) {
        return ragElasticsearchClient.searchAchievements(keyword, topK == null ? 0 : topK);
    }

    private AchievementSearchDoc buildSearchDoc(AchDetailVO detail) {
        LocalDateTime now = LocalDateTime.now();
        JsonNode attachments = detail.getAttachments();
        List<AttachmentMeta> attachmentMetas = parseAttachmentMetas(attachments);
        List<String> attachmentNames = attachmentMetas.stream()
                .map(AttachmentMeta::name)
                .filter(name -> name != null && !name.isBlank())
                .toList();
        long supportedAttachmentCount = attachmentMetas.stream()
                .filter(meta -> meta.mime() != null && SUPPORTED_ATTACHMENT_MIMES.contains(meta.mime()))
                .count();

        Map<String, String> attachmentContents = attachmentContentExtractor.extractContents(attachments);
        String attachmentTextPreview = joinAttachmentContents(attachmentContents);
        String dynamicFieldsText = joinDynamicFields(detail.getFields());
        String keywordsText = String.join(" ", nullToEmptyList(detail.getKeywords()));
        String authorsText = String.join(" ", nullToEmptyList(detail.getAuthors()));
        String attachmentNamesText = String.join(" ", attachmentNames);

        String searchText = joinSearchText(
                detail.getTitle(),
                detail.getTypeName(),
                detail.getTypeCode(),
                truncate(detail.getSummary(), 2000),
                keywordsText,
                authorsText,
                detail.getProjectName(),
                detail.getYear(),
                detail.getVisibilityRange(),
                dynamicFieldsText,
                attachmentNamesText,
                attachmentTextPreview);

        AchievementSearchDoc doc = new AchievementSearchDoc();
        doc.setAchievementDocId(detail.getDocumentId());
        doc.setTitle(detail.getTitle());
        doc.setTypeName(detail.getTypeName());
        doc.setTypeCode(detail.getTypeCode());
        doc.setSummary(truncate(detail.getSummary(), 2000));
        doc.setKeywordsText(keywordsText);
        doc.setAuthorsText(truncate(authorsText, 500));
        doc.setProjectName(detail.getProjectName());
        doc.setYear(detail.getYear());
        doc.setVisibilityRange(detail.getVisibilityRange());
        doc.setAttachmentNames(attachmentNamesText);
        doc.setAttachmentTextPreview(attachmentTextPreview);
        doc.setAttachmentExtractStatus(resolveAttachmentStatus(attachmentNames.size(), supportedAttachmentCount, attachmentContents.size()));
        doc.setAttachmentExtractError(resolveAttachmentError(attachmentNames.size(), supportedAttachmentCount, attachmentContents.size()));
        doc.setSearchText(searchText);
        doc.setSearchHash(md5(searchText));
        doc.setIndexedAt(now);
        doc.setUpdatedAt(now);
        doc.setIsDelete(0);
        return doc;
    }

    private void upsertSearchDoc(AchievementSearchDoc doc) {
        AchievementSearchDoc existing = achievementSearchDocMapper.selectOne(
                new LambdaQueryWrapper<AchievementSearchDoc>()
                        .eq(AchievementSearchDoc::getAchievementDocId, doc.getAchievementDocId())
                        .last("LIMIT 1"));
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            doc.setCreatedAt(now);
            doc.setUpdatedAt(now);
            achievementSearchDocMapper.insert(doc);
        } else {
            doc.setId(existing.getId());
            doc.setCreatedAt(existing.getCreatedAt());
            doc.setEsDocId(existing.getEsDocId());
            doc.setEsIndexedAt(existing.getEsIndexedAt());
            achievementSearchDocMapper.updateById(doc);
        }
    }

    private List<AttachmentMeta> parseAttachmentMetas(JsonNode attachments) {
        List<AttachmentMeta> entries = new ArrayList<>();
        if (attachments == null || attachments.isNull() || attachments.isMissingNode()) {
            return entries;
        }
        JsonNode data = attachments.path("data");
        if (!data.isArray()) {
            return entries;
        }
        for (JsonNode item : data) {
            JsonNode files = item.path("files");
            if (files.isArray()) {
                for (JsonNode file : files) {
                    addAttachmentMeta(entries, file);
                }
            } else if (files.isObject()) {
                addAttachmentMeta(entries, files);
            }
        }
        return entries;
    }

    private void addAttachmentMeta(List<AttachmentMeta> entries, JsonNode file) {
        String name = file.path("name").asText(null);
        if (name == null || name.isBlank()) {
            return;
        }
        entries.add(new AttachmentMeta(name, file.path("mime").asText(null)));
    }

    private String joinAttachmentContents(Map<String, String> attachmentContents) {
        if (attachmentContents == null || attachmentContents.isEmpty()) {
            return null;
        }
        StringJoiner joiner = new StringJoiner("\n\n");
        for (Map.Entry<String, String> entry : attachmentContents.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }
            joiner.add("[" + entry.getKey() + "]\n" + entry.getValue());
        }
        String value = joiner.toString();
        return value.isBlank() ? null : value;
    }

    private String joinDynamicFields(List<AchFieldVO> fields) {
        if (fields == null || fields.isEmpty()) {
            return null;
        }
        StringJoiner joiner = new StringJoiner("\n");
        for (AchFieldVO field : fields) {
            if (field == null || field.getValue() == null) {
                continue;
            }
            String fieldName = field.getFieldName() == null ? "" : field.getFieldName();
            joiner.add(fieldName + "：" + stringify(field.getValue()));
        }
        String value = joiner.toString();
        return value.isBlank() ? null : value;
    }

    private String stringify(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String text) {
            return text;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }

    private String joinSearchText(String... parts) {
        StringJoiner joiner = new StringJoiner("\n");
        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                joiner.add(part.trim());
            }
        }
        return joiner.toString();
    }

    private String resolveAttachmentStatus(int attachmentCount, long supportedCount, int extractedCount) {
        if (attachmentCount == 0 || supportedCount == 0) {
            return "skipped";
        }
        if (extractedCount >= supportedCount) {
            return "success";
        }
        if (extractedCount > 0) {
            return "partial";
        }
        return "failed";
    }

    private String resolveAttachmentError(int attachmentCount, long supportedCount, int extractedCount) {
        if (attachmentCount == 0) {
            return "无附件";
        }
        if (supportedCount == 0) {
            return "无可提取文本的附件类型";
        }
        if (extractedCount == 0) {
            return "支持的附件未提取到文本，详见后端日志";
        }
        if (extractedCount < supportedCount) {
            return "部分附件未提取到文本，详见后端日志";
        }
        return null;
    }

    private List<String> nullToEmptyList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String md5(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(digest.digest((text == null ? "" : text).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 算法不可用", e);
        }
    }

    private record AttachmentMeta(String name, String mime) {
    }
}
