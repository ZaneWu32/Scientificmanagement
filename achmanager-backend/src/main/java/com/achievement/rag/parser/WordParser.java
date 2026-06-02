package com.achievement.rag.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Word 文档解析器（支持 .doc 和 .docx）
 * - 段落保留标题层级
 * - 表格转为 Markdown 格式
 */
@Slf4j
@Component
public class WordParser {

    public ParsedDocument parse(byte[] content, String fileName, boolean isDocx) throws IOException {
        return isDocx ? parseDocx(content, fileName) : parseDoc(content, fileName);
    }

    private ParsedDocument parseDoc(byte[] content, String fileName) throws IOException {
        List<ParsedDocument.TextSegment> segments = new ArrayList<>();

        try (HWPFDocument doc = new HWPFDocument(new ByteArrayInputStream(content));
             WordExtractor extractor = new WordExtractor(doc)) {

            String text = extractor.getText();

            String[] paragraphs = text.split("\n", -1);
            int idx = 0;
            for (String para : paragraphs) {
                String trimmed = para.trim();
                if (!trimmed.isEmpty()) {
                    segments.add(ParsedDocument.TextSegment.builder()
                            .index(idx++)
                            .content(trimmed)
                            .headingLevel(detectHeadingLevel(trimmed))
                            .build());
                }
            }

            return ParsedDocument.builder()
                    .fileName(fileName)
                    .mimeType("application/msword")
                    .fileSize(content.length)
                    .rawText(text)
                    .segments(segments)
                    .build();
        }
    }

    private ParsedDocument parseDocx(byte[] content, String fileName) throws IOException {
        List<ParsedDocument.TextSegment> segments = new ArrayList<>();
        List<ParsedDocument.TableData> tables = new ArrayList<>();

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(content))) {
            StringBuilder rawSb = new StringBuilder();
            int segIdx = 0;

            for (IBodyElement element : doc.getBodyElements()) {
                if (element instanceof XWPFParagraph para) {
                    String text = para.getText();
                    if (text.isBlank()) continue;

                    int headingLevel = 0;
                    String headingText = null;
                    if (para.getStyle() != null && para.getStyle().matches("[1-6]")) {
                        headingLevel = Integer.parseInt(para.getStyle());
                        headingText = text;
                    }

                    rawSb.append(text).append('\n');
                    segments.add(ParsedDocument.TextSegment.builder()
                            .index(segIdx++)
                            .heading(headingText)
                            .headingLevel(headingLevel)
                            .content(text)
                            .build());

                } else if (element instanceof XWPFTable table) {
                    // 表格 → Markdown
                    String md = convertTableToMarkdown(table);
                    rawSb.append(md).append('\n');

                    List<List<String>> rows = new ArrayList<>();
                    for (XWPFTableRow row : table.getRows()) {
                        List<String> cells = new ArrayList<>();
                        for (XWPFTableCell cell : row.getTableCells()) {
                            cells.add(cell.getText().trim());
                        }
                        rows.add(cells);
                    }

                    if (!rows.isEmpty()) {
                        tables.add(ParsedDocument.TableData.builder()
                                .rowCount(rows.size())
                                .colCount(rows.isEmpty() ? 0 : rows.get(0).size())
                                .markdown(md)
                                .rows(rows)
                                .build());
                    }
                }
            }

            log.info("DOCX parsed: {} segments, {} tables", segIdx, tables.size());

            return ParsedDocument.builder()
                    .fileName(fileName)
                    .mimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                    .fileSize(content.length)
                    .rawText(rawSb.toString())
                    .segments(segments)
                    .tables(tables)
                    .build();
        }
    }

    /** XWPFTable → Markdown 表格 */
    private String convertTableToMarkdown(XWPFTable table) {
        StringBuilder sb = new StringBuilder();
        List<XWPFTableRow> rows = table.getRows();
        if (rows.isEmpty()) return "";

        // Header row
        List<String> headerCells = new ArrayList<>();
        for (XWPFTableCell cell : rows.get(0).getTableCells()) {
            headerCells.add(cell.getText().trim());
        }
        sb.append('|').append(String.join("|", headerCells)).append("|\n");
        // Separator
        sb.append('|');
        for (String ignored : headerCells) {
            sb.append("---|");
        }
        sb.append('\n');
        // Data rows
        for (int r = 1; r < rows.size(); r++) {
            List<String> cells = new ArrayList<>();
            for (XWPFTableCell cell : rows.get(r).getTableCells()) {
                cells.add(cell.getText().trim());
            }
            sb.append('|').append(String.join("|", cells)).append("|\n");
        }
        return sb.toString();
    }

    private int detectHeadingLevel(String line) {
        if (line.length() > 0 && line.length() < 80 && !line.endsWith("。") && !line.endsWith(".")) {
            return 1;
        }
        return 0;
    }
}
