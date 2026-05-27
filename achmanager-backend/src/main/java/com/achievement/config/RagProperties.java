package com.achievement.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    private Elasticsearch elasticsearch = new Elasticsearch();
    private Llm llm = new Llm();

    @Data
    public static class Elasticsearch {
        private boolean enabled = true;
        private String baseUrl = "http://127.0.0.1:9200";
        private String username;
        private String password;
        private String achievementIndex = "science_achievement_docs";
        private int searchTopK = 10;
        private int connectTimeoutMs = 3000;
        private int readTimeoutMs = 10000;
    }

    @Data
    public static class Llm {
        private boolean enabled = true;
        private int rerankMaxCandidates = 8;
        private double temperature = 0.1;
        private int maxTokens = 2048;
    }
}
