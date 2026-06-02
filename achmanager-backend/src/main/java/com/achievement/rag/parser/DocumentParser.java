package com.achievement.rag.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 文档解析统一入口——根据 MIME 类型路由到具体解析器
 */
@Slf4j
@Component
public class DocumentParser {

    private final PdfParser pdfParser;
    private final WordParser wordParser;
    private final ExcelParser excelParser;
    private final PptParser pptParser;
    private final PlainTextParser plainTextParser;

    public DocumentParser(PdfParser pdfParser, WordParser wordParser, ExcelParser excelParser,
                          PptParser pptParser, PlainTextParser plainTextParser) {
        this.pdfParser = pdfParser;
        this.wordParser = wordParser;
        this.excelParser = excelParser;
        this.pptParser = pptParser;
        this.plainTextParser = plainTextParser;
    }

    /**
     * 从文件路径解析
     */
    public ParsedDocument parse(Path filePath, String mimeType, String fileName) throws IOException {
        byte[] content = Files.readAllBytes(filePath);
        return parse(content, mimeType, fileName);
    }

    /**
     * 从字节数组解析
     */
    public ParsedDocument parse(byte[] content, String mimeType, String fileName) throws IOException {
        long start = System.currentTimeMillis();

        ParsedDocument result = switch (mimeType) {
            case "application/pdf" ->
                    pdfParser.parse(content, fileName);

            case "application/msword" ->
                    wordParser.parse(content, fileName, false);

            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ->
                    wordParser.parse(content, fileName, true);

            case "application/vnd.ms-excel" ->
                    excelParser.parse(content, fileName, false);

            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ->
                    excelParser.parse(content, fileName, true);

            case "application/vnd.ms-powerpoint" ->
                    pptParser.parse(content, fileName, false);

            case "application/vnd.openxmlformats-officedocument.presentationml.presentation" ->
                    pptParser.parse(content, fileName, true);

            case "text/plain" ->
                    plainTextParser.parse(content, fileName);

            default -> throw new UnsupportedOperationException("不支持的文件类型: " + mimeType);
        };

        long elapsed = System.currentTimeMillis() - start;
        log.info("Document parsed: [{}] {} | {} chars | {}ms", mimeType, fileName,
                result.getRawText() != null ? result.getRawText().length() : 0, elapsed);

        return result;
    }
}
