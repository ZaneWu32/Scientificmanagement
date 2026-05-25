package com.achievement.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Configuration
public class RagElasticsearchConfig {

    @Bean
    public RestClient ragElasticsearchRestClient(RagProperties ragProperties) {
        RagProperties.Elasticsearch props = ragProperties.getElasticsearch();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(props.getConnectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(props.getReadTimeoutMs()));

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        if (StringUtils.hasText(props.getUsername())) {
            builder.defaultHeaders(headers -> headers.setBasicAuth(
                    props.getUsername(),
                    props.getPassword() == null ? "" : props.getPassword()));
        }
        return builder.build();
    }
}
