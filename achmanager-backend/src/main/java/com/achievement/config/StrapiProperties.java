package com.achievement.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "strapi")
public class StrapiProperties {
    private String baseUrl;
    private String apiToken;
}

