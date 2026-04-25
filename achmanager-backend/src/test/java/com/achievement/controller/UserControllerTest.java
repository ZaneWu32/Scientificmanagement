package com.achievement.controller;

import com.achievement.constant.RoleConstants;
import com.achievement.domain.dto.KeycloakUser;
import com.achievement.result.Result;
import com.achievement.service.IKeycloakUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private IKeycloakUserService keycloakUserService;

    @InjectMocks
    private UserController userController;

    @Test
    void getAdminUsersShouldReturnOnlyEnabledAdmins() {
        KeycloakUser enabledAdmin = KeycloakUser.builder()
                .id(1)
                .name("管理员甲")
                .roles(List.of(RoleConstants.RESEARCH_ADMIN))
                .enabled(true)
                .build();
        KeycloakUser disabledAdmin = KeycloakUser.builder()
                .id(2)
                .name("管理员乙")
                .roles(List.of(RoleConstants.RESEARCH_ADMIN))
                .enabled(false)
                .build();

        when(keycloakUserService.getUsersByRole(RoleConstants.RESEARCH_ADMIN))
                .thenReturn(List.of(enabledAdmin, disabledAdmin));

        Result<List<KeycloakUser>> result = userController.getAdminUsers();

        assertNotNull(result);
        assertEquals(1, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        assertEquals(enabledAdmin, result.getData().get(0));
    }
}
