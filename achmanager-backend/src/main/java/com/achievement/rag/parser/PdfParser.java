package com.achievement.rag.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PDF 文档解析器
 */
@Slf4j
@Component
public class PdfParser {

    public ParsedDocument parse(byte[] content, String fileName) throws IOException {
        List<ParsedDocument.TextSegment> segments = new ArrayList<>();
        Map<String, String> metadata = new HashMap<>();

        try (PDDocument doc = Loader.loadPDF(content)) {
            // 元数据
            PDDocumentInformation info = doc.getDocumentInformation();
            if (info.getTitle() != null) metadata.put("title", info.getTitle());
            if (info.getAuthor() != null) metadata.put("author", info.getAuthor());

            // 提取文本（保留段落结构）
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setParagraphStart("\n\n");
            String text = stripper.getText(doc);

            // 按段落分割
            String[] paragraphs = text.split("\n\n", -1);
            int idx = 0;
            for (String para : paragraphs) {
                String trimmed = para.trim().replaceAll("\\n", " ").trim();
                if (!trimmed.isEmpty()) {
                    segments.add(ParsedDocument.TextSegment.builder()
                            .index(idx++)
                            .content(trimmed)
                            .headingLevel(detectHeadingLevel(trimmed))
                            .build());
                }
            }

            log.info("PDF parsed: {} pages, {} segments", doc.getNumberOfPages(), idx);

            return ParsedDocument.builder()
                    .fileName(fileName)
                    .mimeType("application/pdf")
                    .fileSize(content.length)
                    .rawText(text)
                    .segments(segments)
                    .metadata(metadata)
                    .build();
        }
    }

    /** 简单标题探测：短行 + 无句号结尾 */
    private int detectHeadingLevel(String line) {
        if (line.length() > 0 && line.length() < 80 && !line.endsWith("。") && !line.endsWith(".")) {
            return 1;
        }
        return 0;
    }
}
