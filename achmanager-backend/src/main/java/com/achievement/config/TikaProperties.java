package com.achievement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "tika")
public class TikaProperties {

    private long maxFileSizeBytes = 50 * 1024 * 1024;
    private int maxTextLength = 100_000;
    private Ocr ocr = new Ocr();

    @Data
    public static class Ocr {
        private boolean enabled = false;
        private String language = "eng";
        private int timeoutSeconds = 120;
    }
}
