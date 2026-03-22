package com.achievement.controller;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.achievement.annotation.CurrentUser;
import com.achievement.config.KeycloakConfig;
import com.achievement.domain.dto.KeycloakTokenResponse;
import com.achievement.domain.dto.KeycloakUser;
import com.achievement.domain.vo.UserProfileVO;
import com.achievement.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 认证控制器
 * 支持 Keycloak 认证和多角色
 *
 * @author system
 * @since 2026-01-31
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证相关接口")
public class AuthController {

    private final KeycloakConfig keycloakConfig;
    private final Map<String, Long> consumedTicketMap = new ConcurrentHashMap<>();

    @Data
    public static class CodeExchangeRequest {
        private String code;
        private String state;
        private String redirectUri;
        private String codeVerifier;
        private String clientId;
    }

    @Data
    public static class RefreshRequest {
        private String refreshToken;
        private String clientId;
    }

    @Data
    public static class TicketExchangeRequest {
        private String ticket;
        private String state;
        private String clientId;
    }

    /**
     * 获取当前登录用户信息
     * 前端获取 Keycloak token 后，调用此端点获取用户信息
     */
    @Operation(description = "获取当前登录用户信息")
    @GetMapping("/current")
    public Result<UserProfileVO> current(@CurrentUser KeycloakUser currentUser) {
        log.debug("获取当前用户信息: userId={}, username={}, roles={}",
                currentUser.getId(), currentUser.getUsername(), currentUser.getRoles());

        UserProfileVO profile = UserProfileVO.builder()
                .id(currentUser.getId())
                .uuid(currentUser.getUuid())
                .username(currentUser.getUsername())
                .name(currentUser.getName())
                .email(currentUser.getEmail())
                .roles(currentUser.getRoles())
                .build();

        return Result.success(profile);
    }

    /**
     * 退出登录
     * 前端调用此端点执行登出操作（可选）
     * 前端主要通过清除本地 token 实现登出
     */
    @Operation(description = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout(@CurrentUser KeycloakUser currentUser) {
        log.info("用户登出: userId={}, username={}", currentUser.getId(), currentUser.getUsername());

        // 后端可在此处执行一些登出相关的操作，如：
        // - 记录登出日志
        // - 清除会话信息
        // - 将 token 加入黑名单（如果需要）

        return Result.success();
    }

    /**
     * 验证 Token
     * 前端可调用此端点验证当前 token 的有效性
     */
    @Operation(description = "验证 Token")
    @PostMapping("/verify")
    public Result<KeycloakUser> verify(@CurrentUser KeycloakUser currentUser) {
        log.debug("验证 Token: userId={}, username={}", currentUser.getId(), currentUser.getUsername());
        return Result.success(currentUser);
    }

    @Operation(description = "使用授权码交换 token")
    @PostMapping("/exchange-code")
    public Result<KeycloakTokenResponse> exchangeCode(@RequestBody CodeExchangeRequest request) {
        String tokenEndpoint = keycloakConfig.getAuthServerUrl()
                + "/realms/"
                + keycloakConfig.getRealm()
                + "/protocol/openid-connect/token";
        String clientId = (request.getClientId() == null || request.getClientId().isBlank())
                ? keycloakConfig.getClientId()
                : request.getClientId();

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("code", request.getCode());
        body.add("redirect_uri", request.getRedirectUri());
        body.add("code_verifier", request.getCodeVerifier());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<KeycloakTokenResponse> response = restTemplate.postForEntity(
                tokenEndpoint,
                new HttpEntity<>(body, headers),
                KeycloakTokenResponse.class);

        return Result.success(response.getBody());
    }

    @Operation(description = "使用 one-time ticket 交换 token")
    @PostMapping("/exchange-ticket")
    public Result<KeycloakTokenResponse> exchangeTicket(@RequestBody TicketExchangeRequest request) {
        if (request.getTicket() == null || request.getTicket().isBlank()) {
            return Result.error("ticket不能为空");
        }

        Map<String, Object> payload;
        try {
            payload = parseTicketPayload(request.getTicket());
        } catch (IllegalArgumentException e) {
            log.warn("ticket解析失败: {}", e.getMessage());
            return Result.error("ticket格式非法");
        }
        String payloadState = payload.get("state") == null ? null : payload.get("state").toString();
        String code = payload.get("code") == null ? null : payload.get("code").toString();
        String codeVerifier = payload.get("codeVerifier") == null ? null : payload.get("codeVerifier").toString();
        String redirectUri = payload.get("redirectUri") == null ? null : payload.get("redirectUri").toString();
        String jti = payload.get("jti") == null ? null : payload.get("jti").toString();
        Long exp = payload.get("exp") == null ? null : Long.valueOf(payload.get("exp").toString());

        if (payloadState == null || code == null || codeVerifier == null || redirectUri == null || jti == null
                || exp == null) {
            return Result.error("ticket格式非法");
        }
        if (request.getState() != null && !request.getState().isBlank() && !request.getState().equals(payloadState)) {
            return Result.error("ticket状态校验失败");
        }

        long now = Instant.now().getEpochSecond();
        if (exp < now) {
            return Result.error("ticket已过期");
        }

        cleanupConsumedTickets(now);
        if (consumedTicketMap.putIfAbsent(jti, exp) != null) {
            return Result.error("ticket已被使用");
        }

        CodeExchangeRequest exchangeRequest = new CodeExchangeRequest();
        exchangeRequest.setCode(code);
        exchangeRequest.setCodeVerifier(codeVerifier);
        exchangeRequest.setRedirectUri(redirectUri);
        exchangeRequest.setClientId(request.getClientId());
        return exchangeCode(exchangeRequest);
    }

    @Operation(description = "刷新 token")
    @PostMapping("/refresh")
    public Result<KeycloakTokenResponse> refresh(@RequestBody RefreshRequest request) {
        String tokenEndpoint = keycloakConfig.getAuthServerUrl()
                + "/realms/"
                + keycloakConfig.getRealm()
                + "/protocol/openid-connect/token";
        String clientId = (request.getClientId() == null || request.getClientId().isBlank())
                ? keycloakConfig.getClientId()
                : request.getClientId();

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("client_id", clientId);
        body.add("refresh_token", request.getRefreshToken());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<KeycloakTokenResponse> response = restTemplate.postForEntity(
                tokenEndpoint,
                new HttpEntity<>(body, headers),
                KeycloakTokenResponse.class);

        return Result.success(response.getBody());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseTicketPayload(String ticket) {
        try {
            String base64 = ticket.replace('-', '+').replace('_', '/');
            int padding = (4 - base64.length() % 4) % 4;
            base64 = base64 + "=".repeat(padding);
            byte[] decoded = Base64.getDecoder().decode(base64);
            String json = new String(decoded, StandardCharsets.UTF_8);
            return new ObjectMapper().readValue(json, Map.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("ticket解析失败", e);
        }
    }

    private void cleanupConsumedTickets(long nowEpochSeconds) {
        consumedTicketMap.entrySet().removeIf(entry -> entry.getValue() < nowEpochSeconds);
    }
}
