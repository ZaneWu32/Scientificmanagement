package com.achievement.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import com.achievement.service.ITextExtractionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class AttachmentContentExtractorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ITextExtractionService textExtractionService;

    @Test
    void extractContentsShouldParseCommonStrapiMediaShapes() throws Exception {
        List<URI> requestedUris = new ArrayList<>();
        WebClient webClient = WebClient.builder()
                .baseUrl("http://strapi.test")
                .exchangeFunction(request -> {
                    requestedUris.add(request.url());
                    byte[] body = "mock file bytes".getBytes(StandardCharsets.UTF_8);
                    return Mono.just(ClientResponse.create(HttpStatus.OK)
                            .body(Flux.just(new DefaultDataBufferFactory().wrap(body)))
                            .build());
                })
                .build();

        when(textExtractionService.extractText(any(byte[].class), eq("proposal.pdf"), eq("application/pdf")))
                .thenReturn(new ITextExtractionService.ExtractionResult(
                        "proposal extracted text", "application/pdf", false, null, null));
        when(textExtractionService.extractText(any(byte[].class), eq("brief.txt"), eq("text/plain")))
                .thenReturn(new ITextExtractionService.ExtractionResult(
                        "brief extracted text", "text/plain", false, null, null));

        AttachmentContentExtractor extractor =
                new AttachmentContentExtractor(webClient, textExtractionService);
        JsonNode attachments = objectMapper.readTree("""
                {
                  "data": [
                    {
                      "id": 1,
                      "attributes": {
                        "files": {
                          "data": [
                            {
                              "id": 11,
                              "attributes": {
                                "name": "proposal.pdf",
                                "url": "/uploads/proposal.pdf",
                                "mime": "application/pdf",
                                "size": 12.5
                              }
                            }
                          ]
                        }
                      }
                    },
                    {
                      "id": 2,
                      "files": {
                        "data": {
                          "id": 12,
                          "attributes": {
                            "name": "brief.txt",
                            "url": "/uploads/brief.txt",
                            "mime": "text/plain",
                            "size": 1
                          }
                        }
                      }
                    }
                  ]
                }
                """);

        Map<String, String> contents = extractor.extractContents(attachments);

        assertEquals("proposal extracted text", contents.get("proposal.pdf"));
        assertEquals("brief extracted text", contents.get("brief.txt"));
        assertEquals(2, requestedUris.size());
        assertTrue(requestedUris.stream().anyMatch(uri -> "/uploads/proposal.pdf".equals(uri.getPath())));
        assertTrue(requestedUris.stream().anyMatch(uri -> "/uploads/brief.txt".equals(uri.getPath())));
        verify(textExtractionService).extractText(any(byte[].class), eq("proposal.pdf"), eq("application/pdf"));
        verify(textExtractionService).extractText(any(byte[].class), eq("brief.txt"), eq("text/plain"));
    }
}
