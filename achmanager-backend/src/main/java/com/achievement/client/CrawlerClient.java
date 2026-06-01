package com.achievement.client;

import com.achievement.domain.dto.CrawlerResultDTO;
import com.achievement.domain.vo.CrawlerStatsVO;
import com.achievement.domain.vo.CrawlerStatusVO;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlerClient {

    private final WebClient crawlerWebClient;

    public Map<String, String> listCrawlers() {
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

            Map<String, String> result = new LinkedHashMap<>();
            if (body != null && body.has("crawlers")) {
                JsonNode crawlers = body.get("crawlers");
                crawlers.fieldNames().forEachRemaining(id -> result.put(id, crawlers.get(id).asText(id)));
            }
            return result;
        } catch (Exception e) {
            log.error("无法连接爬虫服务: {}", e.getMessage());
            return Map.of();
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

    public Map<String, CrawlerStatusVO> getAllStatuses() {
        try {
            JsonNode body = crawlerWebClient.get()
                    .uri("/api/crawlers/status")
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, resp ->
                            resp.bodyToMono(String.class)
                                    .doOnNext(b -> log.error("Crawler getAllStatuses error: {}", b))
                                    .then(resp.createException()))
                    .bodyToMono(JsonNode.class)
                    .block();

            Map<String, CrawlerStatusVO> result = new LinkedHashMap<>();
            if (body != null && body.has("tasks")) {
                JsonNode tasks = body.get("tasks");
                tasks.fieldNames().forEachRemaining(id -> {
                    JsonNode task = tasks.get(id);
                    CrawlerStatusVO vo = new CrawlerStatusVO();
                    vo.setId(id);
                    vo.setStatus(task.path("status").asText("unknown"));
                    JsonNode statsNode = task.get("stats");
                    if (statsNode != null) {
                        CrawlerStatsVO stats = new CrawlerStatsVO();
                        stats.setTotal(statsNode.path("total").asInt(0));
                        stats.setSuccess(statsNode.path("success").asInt(0));
                        stats.setFailed(statsNode.path("failed").asInt(0));
                        stats.setDone(statsNode.path("done").asInt(0));
                        vo.setStats(stats);
                    }
                    result.put(id, vo);
                });
            }
            return result;
        } catch (Exception e) {
            log.error("获取爬虫状态列表失败: {}", e.getMessage());
            return Map.of();
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
