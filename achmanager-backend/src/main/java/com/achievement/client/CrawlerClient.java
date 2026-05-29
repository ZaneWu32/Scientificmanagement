package com.achievement.client;

import com.achievement.domain.dto.CrawlerResultDTO;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlerClient {

    private final WebClient crawlerWebClient;

    public List<String> listCrawlers() {
        try {
            JsonNode body = crawlerWebClient.get()
                    .uri("/api/crawlers")
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, resp ->
                            resp.bodyToMono(String.class)
                                    .doOnNext(b -> log.error("Crawler listCrawlers error: {}", b))
                                    .then(resp.createException()))
                    .bodyToMono(JsonNode.class)
                    .block();

            List<String> result = new ArrayList<>();
            if (body != null && body.has("crawlers")) {
                body.get("crawlers").fieldNames().forEachRemaining(result::add);
            }
            return result;
        } catch (Exception e) {
            log.error("无法连接爬虫服务: {}", e.getMessage());
            return List.of();
        }
    }

    public boolean startCrawl(String crawlerId) {
        try {
            crawlerWebClient.post()
                    .uri("/api/crawler/{id}/start", crawlerId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, resp ->
                            resp.bodyToMono(String.class)
                                    .doOnNext(b -> log.error("Crawler startCrawl error for {}: {}", crawlerId, b))
                                    .then(resp.createException()))
                    .bodyToMono(String.class)
                    .block();
            return true;
        } catch (Exception e) {
            log.error("启动爬虫 {} 失败: {}", crawlerId, e.getMessage());
            return false;
        }
    }

    public String getStatus(String crawlerId) {
        try {
            JsonNode body = crawlerWebClient.get()
                    .uri("/api/crawler/{id}/status", crawlerId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, resp ->
                            resp.bodyToMono(String.class)
                                    .doOnNext(b -> log.error("Crawler getStatus error for {}: {}", crawlerId, b))
                                    .then(resp.createException()))
                    .bodyToMono(JsonNode.class)
                    .block();

            return body != null ? body.path("status").asText("unknown") : "unknown";
        } catch (Exception e) {
            log.error("获取爬虫 {} 状态失败: {}", crawlerId, e.getMessage());
            return "unavailable";
        }
    }

    public List<CrawlerResultDTO> getResults(String crawlerId) {
        try {
            JsonNode body = crawlerWebClient.get()
                    .uri("/api/crawler/{id}/result", crawlerId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, resp ->
                            resp.bodyToMono(String.class)
                                    .doOnNext(b -> log.error("Crawler getResults error for {}: {}", crawlerId, b))
                                    .then(resp.createException()))
                    .bodyToMono(JsonNode.class)
                    .block();

            List<CrawlerResultDTO> results = new ArrayList<>();
            if (body != null && body.has("results")) {
                for (JsonNode node : body.get("results")) {
                    CrawlerResultDTO dto = new CrawlerResultDTO();
                    dto.setSource(node.path("source").asText(""));
                    dto.setTitle(node.path("title").asText(""));
                    dto.setDatetime(node.path("datetime").asText(""));
                    dto.setContent(node.path("content").asText(""));
                    List<String> hrefs = new ArrayList<>();
                    if (node.has("hrefs")) {
                        for (JsonNode href : node.get("hrefs")) {
                            hrefs.add(href.asText());
                        }
                    }
                    dto.setHrefs(hrefs);
                    results.add(dto);
                }
            }
            return results;
        } catch (Exception e) {
            log.error("获取爬虫 {} 结果失败: {}", crawlerId, e.getMessage());
            return List.of();
        }
    }
}
