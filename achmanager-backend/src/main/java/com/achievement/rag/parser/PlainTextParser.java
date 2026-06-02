package com.achievement.rag.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 纯文本解析器
 */
@Slf4j
@Component
public class PlainTextParser {

    public ParsedDocument parse(byte[] content, String fileName) {
        String text = new String(content, StandardCharsets.UTF_8);

        String[] lines = text.split("\n", -1);
        List<ParsedDocument.TextSegment> segments = new ArrayList<>();
        int idx = 0;
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                segments.add(ParsedDocument.TextSegment.builder()
                        .index(idx++)
                        .content(trimmed)
                        .build());
            }
        }

        log.info("TXT parsed: {} segments", idx);

        return ParsedDocument.builder()
                .fileName(fileName)
                .mimeType("text/plain")
                .fileSize(content.length)
                .rawText(text)
                .segments(segments)
                .build();
    }
}
