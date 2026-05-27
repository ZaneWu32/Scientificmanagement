package com.achievement.service;

import java.io.InputStream;

public interface ITextExtractionService {

    ExtractionResult extractText(byte[] content, String filename, String mimeType);

    ExtractionResult extractText(InputStream stream, String filename, String mimeType);

    record ExtractionResult(
            String text,
            String detectedMimeType,
            boolean truncated,
            String title,
            String author) {
    }
}
