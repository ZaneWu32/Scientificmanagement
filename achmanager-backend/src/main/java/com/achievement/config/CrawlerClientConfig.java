package com.achievement.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.ExchangeStrategies;

@Configuration
@RequiredArgsConstructor
public class CrawlerClientConfig {

    private static final int MAX_BUFFER_SIZE = 16 * 1024 * 1024; // 16MB

    private final CrawlerProperties props;

    @Bean
    public WebClient crawlerWebClient() {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(MAX_BUFFER_SIZE))
                .build();
        return WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .exchangeStrategies(strategies)
                .build();
    }
}
