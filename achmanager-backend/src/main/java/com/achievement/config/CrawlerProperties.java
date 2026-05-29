package com.achievement.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "crawler")
public class CrawlerProperties {
    private String baseUrl = "http://localhost:8020";
    private int pollIntervalMs = 3000;
    private int maxPollAttempts = 100;
    private String syncCron = "0 0 2 * * ?";
}
