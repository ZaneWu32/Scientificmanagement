package com.achievement.client;

import com.achievement.config.LlmProperties;
import com.achievement.constant.LlmUsage;
import com.achievement.domain.ChatMessage;
import com.achievement.exception.LlmException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class LlmClient {

    private final LlmProperties llmProperties;

    // ── Chat Completions (非流式) ──

    public String chatCompletion(LlmUsage usage, List<ChatMessage> messages) {
        return chatCompletion(usage, messages, null, null);
    }

    public String chatCompletion(LlmUsage usage, List<ChatMessage> messages,
                                 Double temperature, Integer maxTokens) {
        LlmProperties.ModelConfig config = resolveConfig(usage);
        WebClient client = buildClient(config);

        Map<String, Object> body = buildRequestBody(config, messages, temperature, maxTokens, false);

        return client.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class)
                                .flatMap(err -> Mono.error(new LlmException(
                                        resp.statusCode().value(),
                                        "LLM API error (" + usage.getValue() + "): " + err))))
                .bodyToMono(String.class)
                .block();
    }

    // ── Chat Completions (流式) ──

    public Flux<String> chatCompletionStream(LlmUsage usage, List<ChatMessage> messages) {
        LlmProperties.ModelConfig config = resolveConfig(usage);
        WebClient client = buildClient(config);

        Map<String, Object> body = buildRequestBody(config, messages, null, null, true);

        return client.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class)
                                .flatMap(err -> Mono.error(new LlmException(
                                        resp.statusCode().value(),
                                        "LLM streaming API error (" + usage.getValue() + "): " + err))))
                .bodyToFlux(String.class);
    }

    // ── Embeddings ──

    public String createEmbedding(LlmUsage usage, String input) {
        return createEmbedding(usage, List.of(input));
    }

    public String createEmbedding(LlmUsage usage, List<String> inputs) {
        LlmProperties.ModelConfig config = resolveConfig(usage);
        WebClient client = buildClient(config);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.getModelName());
        body.put("input", inputs.size() == 1 ? inputs.get(0) : inputs);

        return client.post()
                .uri("/embeddings")
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class)
                                .flatMap(err -> Mono.error(new LlmException(
                                        resp.statusCode().value(),
                                        "LLM embeddings API error (" + usage.getValue() + "): " + err))))
                .bodyToMono(String.class)
                .block();
    }

    // ── 内部方法 ──

    private LlmProperties.ModelConfig resolveConfig(LlmUsage usage) {
        if (llmProperties.getUsage() == null || llmProperties.getModels() == null) {
            throw new LlmException("LLM not configured");
        }
        String modelId = llmProperties.getUsage().get(usage.getValue());
        if (modelId == null) {
            throw new LlmException("LLM usage not configured: " + usage.getValue());
        }
        LlmProperties.ModelConfig config = llmProperties.getModels().get(modelId);
        if (config == null) {
            throw new LlmException("LLM model not found: " + modelId);
        }
        return config;
    }

    private WebClient buildClient(LlmProperties.ModelConfig config) {
        return WebClient.builder()
                .baseUrl(config.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private Map<String, Object> buildRequestBody(LlmProperties.ModelConfig config,
                                                  List<ChatMessage> messages,
                                                  Double temperature, Integer maxTokens,
                                                  boolean stream) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.getModelName());
        body.put("messages", messages.stream()
                .map(m -> Map.<String, Object>of("role", m.role(), "content", m.content()))
                .toList());
        body.put("temperature", temperature != null ? temperature : config.getTemperature());
        body.put("max_tokens", maxTokens != null ? maxTokens : config.getMaxTokens());
        body.put("stream", stream);
        if (config.getTopP() != null) {
            body.put("top_p", config.getTopP());
        }
        return body;
    }
}
