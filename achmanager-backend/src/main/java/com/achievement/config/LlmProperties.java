package com.achievement.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {

    /** 可用模型池，key 为模型 ID */
    private Map<String, ModelConfig> models;

    /** 用途→模型 ID 映射，key 为用途标识（如 "default"），value 为 models 中的 key */
    private Map<String, String> usage;

    @Data
    public static class ModelConfig {
        private String baseUrl;
        private String apiKey;
        private String modelName;
        private int maxTokens = 4096;
        private double temperature = 0.7;
        private Integer topP;
        private String apiType;
    }
}
