package com.achievement.utils;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.achievement.exception.TextExtractionException;
import com.achievement.service.ITextExtractionService;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentContentExtractor {

    private final WebClient strapiWebClient;
    private final ITextExtractionService textExtractionService;

    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofSeconds(30);

    private record FileEntry(String name, String mime, String url, long sizeBytes) {}

    /**
     * 从附件 JSON 中提取每个文件的文本内容。
     * 返回 Map&lt;文件名, 提取文本&gt;，失败/跳过的文件不会出现在结果中。
     */
    public Map<String, String> extractContents(JsonNode attachments) {
        Map<String, String> result = new LinkedHashMap<>();
        List<FileEntry> entries = parseFileEntries(attachments);
        if (entries.isEmpty()) {
            return result;
        }
        log.info("开始提取附件内容，附件数量={}", entries.size());
        for (FileEntry entry : entries) {
            try {
                byte[] content = downloadFile(entry.url());
                if (content == null || content.length == 0) {
                    log.info("附件文件为空，跳过: 文件名={}", entry.name());
                    continue;
                }
                ITextExtractionService.ExtractionResult extractionResult =
                        textExtractionService.extractText(content, entry.name(), entry.mime());
                String text = extractionResult.text();
                if (text != null && !text.isBlank()) {
                    result.put(entry.name(), text);
                    log.info("附件内容提取成功: 文件名={}, 提取长度={}", entry.name(), text.length());
                }
            } catch (TextExtractionException e) {
                log.warn("附件内容提取失败，跳过该文件: 文件名={}, 错误={}", entry.name(), e.getMessage());
            } catch (Exception e) {
                log.warn("附件下载或处理失败，跳过该文件: 文件名={}, 错误={}", entry.name(), e.getMessage());
            }
        }
        log.info("附件内容提取完成，成功={}/{}", result.size(), entries.size());
        return result;
    }

    private List<FileEntry> parseFileEntries(JsonNode attachments) {
        List<FileEntry> entries = new java.util.ArrayList<>();
        if (attachments == null || attachments.isMissingNode() || attachments.isNull()) {
            return entries;
        }
        JsonNode data = attachments.path("data");
        if (!data.isArray()) {
            return entries;
        }
        for (JsonNode entry : data) {
            JsonNode filesNode = entry.path("files");
            if (filesNode.isArray()) {
                for (JsonNode file : filesNode) {
                    addFileEntry(entries, file);
                }
            } else if (filesNode.isObject()) {
                addFileEntry(entries, filesNode);
            }
        }
        return entries;
    }

    private void addFileEntry(List<FileEntry> entries, JsonNode file) {
        String url = file.path("url").asText(null);
        String name = file.path("name").asText(null);
        if (url == null || url.isBlank() || name == null || name.isBlank()) {
            return;
        }
        String mime = file.path("mime").asText(null);
        long sizeBytes = (long) (file.path("size").asDouble(0) * 1024);
        entries.add(new FileEntry(name, mime, url, sizeBytes));
    }

    private byte[] downloadFile(String url) {
        return strapiWebClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(byte[].class)
                .block(DOWNLOAD_TIMEOUT);
    }
}
