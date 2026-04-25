package com.achievement.service.impl;

import com.achievement.config.KeycloakConfig;
import com.achievement.constant.RoleConstants;
import com.achievement.domain.dto.KeycloakUser;
import com.achievement.domain.po.BusinessUser;
import com.achievement.mapper.BusinessUserMapper;
import com.achievement.utils.TwoLevelCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeycloakUserServiceImplTest {

    @Mock
    private Keycloak keycloak;

    @Mock
    private KeycloakConfig keycloakConfig;

    @Mock
    private BusinessUserMapper businessUserMapper;

    @Mock
    private TwoLevelCache twoLevelCache;

    @Mock
    private RealmResource realmResource;

    @Mock
    private UsersResource usersResource;

    @Mock
    private UserResource userResource;

    @Mock
    private RoleMappingResource roleMappingResource;

    @Mock
    private RoleScopeResource roleScopeResource;

    @InjectMocks
    private KeycloakUserServiceImpl keycloakUserService;

    @Test
    void getUserByIdShouldResolveEffectiveRealmRolesWhenRepresentationOmitsRoles() {
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setId("kc-user-1");
        userRepresentation.setUsername("research_admin");
        userRepresentation.setEnabled(true);

        RoleRepresentation adminRole = new RoleRepresentation();
        adminRole.setName(RoleConstants.RESEARCH_ADMIN);
        RoleRepresentation defaultRole = new RoleRepresentation();
        defaultRole.setName(RoleConstants.DEFAULT_ROLE);

        when(keycloakConfig.getRealm()).thenReturn("research");
        when(keycloakConfig.isDefaultRole(anyString())).thenAnswer(invocation ->
                RoleConstants.DEFAULT_ROLE.equals(invocation.getArgument(0)));
        when(keycloak.realm("research")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get("kc-user-1")).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(userRepresentation);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);
        when(roleScopeResource.listEffective()).thenReturn(List.of(adminRole, defaultRole));
        when(businessUserMapper.selectOne(any())).thenReturn(new BusinessUser()
                .setId(42)
                .setKeycloakUserId("kc-user-1"));

        KeycloakUser user = keycloakUserService.getUserById("kc-user-1");

        assertNotNull(user);
        assertEquals(42, user.getId());
        assertEquals("research_admin", user.getUsername());
        assertEquals(List.of(RoleConstants.RESEARCH_ADMIN), user.getRoles());
        assertTrue(user.isEnabled());
    }
}
