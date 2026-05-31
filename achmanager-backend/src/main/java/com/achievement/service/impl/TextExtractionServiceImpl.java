package com.achievement.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

import org.apache.tika.exception.TikaException;
import org.apache.tika.exception.WriteLimitReachedException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import com.achievement.config.TikaProperties;
import com.achievement.exception.TextExtractionException;
import com.achievement.service.ITextExtractionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TextExtractionServiceImpl implements ITextExtractionService {

    private final TikaProperties tikaProperties;
    private final AutoDetectParser parser = new AutoDetectParser();

    @Override
    public ExtractionResult extractText(byte[] content, String filename, String mimeType) {
        if (content == null || content.length == 0) {
            return emptyResult();
        }
        long maxBytes = tikaProperties.getMaxFileSizeBytes();
        if (content.length > maxBytes) {
            throw new TextExtractionException(
                    "文件大小 %d 字节超过限制 %d 字节".formatted(content.length, maxBytes));
        }
        return extractText(() -> TikaInputStream.get(content), filename, mimeType);
    }

    @Override
    public ExtractionResult extractText(InputStream stream, String filename, String mimeType) {
        if (stream == null) {
            return emptyResult();
        }
        return extractText(() -> TikaInputStream.get(stream), filename, mimeType);
    }

    private ExtractionResult extractText(Supplier<TikaInputStream> tisSupplier, String filename, String mimeType) {
        Metadata metadata = buildMetadata(filename, mimeType);
        try (TikaInputStream tis = tisSupplier.get()) {
            return doExtract(tis, metadata);
        } catch (TikaException | IOException | SAXException e) {
            throw new TextExtractionException("文本提取失败: " + filename, e);
        } catch (RuntimeException e) {
            if (isOcrUnavailable(e)) {
                log.warn("Tesseract 不可用，OCR 跳过: {}", filename);
                return emptyResult();
            }
            throw e;
        }
    }

    private ExtractionResult doExtract(TikaInputStream tis, Metadata metadata)
            throws TikaException, IOException, SAXException {
        int maxLen = tikaProperties.getMaxTextLength();
        BodyContentHandler handler = new BodyContentHandler(maxLen);
        ParseContext context = new ParseContext();

        TikaProperties.Ocr ocrConfig = tikaProperties.getOcr();
        if (ocrConfig.isEnabled()) {
            TesseractOCRConfig tessConfig = new TesseractOCRConfig();
            tessConfig.setLanguage(ocrConfig.getLanguage());
            tessConfig.setTimeoutSeconds(ocrConfig.getTimeoutSeconds());
            context.set(TesseractOCRConfig.class, tessConfig);
        }

        boolean truncated = false;
        try {
            parser.parse(tis, handler, metadata, context);
        } catch (WriteLimitReachedException e) {
            truncated = true;
        }

        String detectedType = metadata.get(Metadata.CONTENT_TYPE);
        if (!ocrConfig.isEnabled() && detectedType != null && detectedType.startsWith("image/")) {
            log.info("OCR 未启用，跳过图片文件: detectedType={}", detectedType);
            return new ExtractionResult(null, detectedType, false,
                    metadata.get(TikaCoreProperties.TITLE),
                    metadata.get(TikaCoreProperties.CREATOR));
        }

        String text = handler.toString();
        String title = metadata.get(TikaCoreProperties.TITLE);
        String author = metadata.get(TikaCoreProperties.CREATOR);
        return new ExtractionResult(text, detectedType, truncated, title, author);
    }

    private Metadata buildMetadata(String filename, String mimeType) {
        Metadata metadata = new Metadata();
        if (filename != null && !filename.isBlank()) {
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, filename);
        }
        if (mimeType != null && !mimeType.isBlank()) {
            metadata.set(Metadata.CONTENT_TYPE, mimeType);
        }
        return metadata;
    }

    private ExtractionResult emptyResult() {
        return new ExtractionResult(null, null, false, null, null);
    }

    private boolean isOcrUnavailable(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            String msg = t.getMessage();
            if (msg != null && (msg.contains("Tesseract") || msg.contains("tesseract")
                    || msg.contains("OCR"))) {
                return true;
            }
        }
        return false;
    }
}
