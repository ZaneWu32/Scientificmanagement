package com.achievement.service;

import java.util.List;
import java.util.Optional;

import com.achievement.domain.dto.KeycloakUser;

public interface IKeycloakUserService {

    KeycloakUser getUserById(String keycloakUserId);

    KeycloakUser getUserById(Integer userId);

    List<KeycloakUser> getUsersByRole(String role);

    List<KeycloakUser> searchUsers(String query);

    List<KeycloakUser> getAllUsers();

    Integer getOrCreateUserId(String keycloakUserId);

    Optional<String> getUserRealName(String userId);

    /**
     * 注销用户在 Keycloak 的会话
     *
     * @param userId Keycloak 用户 ID
     * @return 是否注销成功
     */
    boolean logoutUserSessions(String userId);
}
