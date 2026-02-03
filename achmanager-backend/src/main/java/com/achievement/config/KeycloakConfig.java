package com.achievement.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import lombok.Data;

/**
 * Keycloak 配置类
 *
 * @author wyf7685
 * @since 2026-01-31
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakConfig {
    private String authServerUrl;
    private String realm;
    private String clientId;
    private String clientSecret;
    private String realmDefaultRoles;

    public String getJwkSetUri() {
        return authServerUrl + "/realms/" + realm + "/protocol/openid-connect/certs";
    }

    @Bean
    public Keycloak keycloak() {
        return KeycloakBuilder.builder()
                .serverUrl(authServerUrl)
                .realm(realm)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .build();
    }

    public boolean isDefaultRole(String role) {
        return role != null && realmDefaultRoles.contains(role);
    }

    public Converter<Jwt, AbstractAuthenticationToken> getKeycloakJwtConverter() {
        return (jwt) -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            // 从 realm_access 中提取 Realm 级别的角色
            if (jwt.getClaim("realm_access") instanceof Map realmAccess
                    && realmAccess.get("roles") instanceof List roles) {
                for (Object roleObj : roles) {
                    if (roleObj instanceof String role && !isDefaultRole(role)) {
                        // 添加 ROLE_ 前缀以便与 Spring Security 的权限检查兼容
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.trim().toUpperCase()));
                    }
                }
            }
            return new JwtAuthenticationToken(jwt, authorities);
        };
    }
}
