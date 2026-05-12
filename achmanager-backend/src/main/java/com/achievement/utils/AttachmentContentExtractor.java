package com.achievement.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentContentExtractor {

    private final WebClient strapiWebClient;

    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    private static final int MAX_TEXT_LENGTH = 5000;
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
                if (entry.sizeBytes() > MAX_FILE_SIZE_BYTES) {
                    log.warn("附件文件过大，跳过提取: 文件名={}, 大小={}bytes", entry.name(), entry.sizeBytes());
                    continue;
                }
                if (entry.mime() == null || isUnsupportedMime(entry.mime())) {
                    log.info("附件类型不支持文本提取，跳过: 文件名={}, mime={}", entry.name(), entry.mime());
                    continue;
                }
                byte[] content = downloadFile(entry.url());
                String text = extractText(content, entry.mime());
                if (text != null && !text.isBlank()) {
                    result.put(entry.name(), text);
                    log.info("附件内容提取成功: 文件名={}, 提取长度={}", entry.name(), text.length());
                }
            } catch (Exception e) {
                log.warn("附件内容提取失败，跳过该文件: 文件名={}, 错误={}", entry.name(), e.getMessage());
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

    private String extractText(byte[] content, String mime) throws IOException {
        if (content == null || content.length == 0) {
            return null;
        }
        return switch (mime) {
            case "text/plain" -> truncate(new String(content, StandardCharsets.UTF_8));
            case "application/pdf" -> extractPdfText(content);
            case "application/msword" -> extractDocText(content);
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ->
                    extractDocxText(content);
            case "application/vnd.ms-excel" -> extractExcelText(content, false);
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ->
                    extractExcelText(content, true);
            default -> null;
        };
    }

    private String extractPdfText(byte[] content) throws IOException {
        try (PDDocument doc = Loader.loadPDF(content)) {
            return truncate(new PDFTextStripper().getText(doc));
        }
    }

    private String extractDocText(byte[] content) throws IOException {
        try (HWPFDocument doc = new HWPFDocument(new ByteArrayInputStream(content));
             WordExtractor extractor = new WordExtractor(doc)) {
            return truncate(extractor.getText());
        }
    }

    private String extractDocxText(byte[] content) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(content))) {
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph para : doc.getParagraphs()) {
                sb.append(para.getText()).append('\n');
            }
            return truncate(sb.toString());
        }
    }

    private String extractExcelText(byte[] content, boolean xlsx) throws IOException {
        try (Workbook wb = xlsx
                ? new XSSFWorkbook(new ByteArrayInputStream(content))
                : new HSSFWorkbook(new ByteArrayInputStream(content))) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                Sheet sheet = wb.getSheetAt(i);
                if (wb.getNumberOfSheets() > 1) {
                    sb.append("[Sheet: ").append(sheet.getSheetName()).append("]\n");
                }
                for (Row row : sheet) {
                    for (int c = 0; c < row.getLastCellNum(); c++) {
                        Cell cell = row.getCell(c);
                        if (cell != null) {
                            sb.append(cell.toString());
                        }
                        if (c < row.getLastCellNum() - 1) {
                            sb.append('\t');
                        }
                    }
                    sb.append('\n');
                }
            }
            return truncate(sb.toString());
        }
    }

    private boolean isUnsupportedMime(String mime) {
        return mime.startsWith("image/") || mime.startsWith("video/") || mime.startsWith("audio/")
                || "application/octet-stream".equals(mime) || "application/zip".equals(mime);
    }

    private String truncate(String text) {
        if (text == null) {
            return null;
        }
        if (text.length() > MAX_TEXT_LENGTH) {
            return text.substring(0, MAX_TEXT_LENGTH) + "\n...(已截断)";
        }
        return text;
    }
}
