package com.achievement.client;

import com.achievement.result.StrapiDataReq;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class StrapiClient {

    private final WebClient strapiWebClient;


    //新增成果物类型，只对成果物类型表进行操作

    // ✅ 新增：POST /api/{collection}  body: {"data":{...}}
    public String create(String collection, Map<String, Object> data) {
        return strapiWebClient.post()
                .uri("/api/{collection}", collection)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(data)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class)
                                .flatMap(err -> Mono.error(new RuntimeException("Strapi error: " + err)))
                )
                .bodyToMono(String.class)
                .block();
    }

    // ✅更新：PUT /api/{collection}/{id}  body: {"data":{...}}
    public String update(String collection, Object id, Map<String, Object> data) {
        return strapiWebClient.put()
                .uri("/api/{collection}/{id}", collection, id)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(data)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class)
                                .flatMap(err -> Mono.error(new RuntimeException("Strapi error: " + err)))
                )
                .bodyToMono(String.class)
                .block();
    }
}
