package com.achievement.service.impl;

import com.achievement.constant.RoleConstants;
import com.achievement.domain.dto.AssignReviewerDTO;
import com.achievement.domain.dto.KeycloakUser;
import com.achievement.domain.dto.ReviewRequestDTO;
import com.achievement.domain.po.AchievementMains;
import com.achievement.domain.po.AchievementReviewerAssignment;
import com.achievement.domain.vo.RagIndexResultVO;
import com.achievement.domain.vo.ReviewResultVO;
import com.achievement.mapper.AchievementMainsMapper;
import com.achievement.mapper.AchievementReviewMapper;
import com.achievement.mapper.AchievementReviewerAssignmentMapper;
import com.achievement.service.IKeycloakUserService;
import com.achievement.service.IRagAchievementIndexService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AchievementReviewServiceImplTest {

    @Mock
    private AchievementMainsMapper achievementMainsMapper;

    @Mock
    private AchievementReviewMapper achievementReviewMapper;

    @Mock
    private AchievementReviewerAssignmentMapper assignmentMapper;

    @Mock
    private IKeycloakUserService keycloakUserService;

    @Mock
    private IRagAchievementIndexService ragAchievementIndexService;

    @InjectMocks
    private AchievementReviewServiceImpl achievementReviewService;

    @BeforeEach
    void setUpTableInfo() {
        if (TableInfoHelper.getTableInfo(AchievementMains.class) == null) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                    AchievementMains.class);
        }
        if (TableInfoHelper.getTableInfo(AchievementReviewerAssignment.class) == null) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                    AchievementReviewerAssignment.class);
        }
    }

    @Test
    void assignReviewersShouldAllowResearchAdminAsReviewer() {
        AchievementMains achievement = new AchievementMains()
                .setId(10)
                .setDocumentId("ach-doc-1")
                .setPublishedAt(LocalDateTime.now())
                .setIsDelete(0);
        KeycloakUser assigner = KeycloakUser.builder()
                .id(100)
                .uuid("assigner-uuid")
                .name("分配管理员")
                .roles(List.of(RoleConstants.RESEARCH_ADMIN))
                .enabled(true)
                .build();
        KeycloakUser reviewer = KeycloakUser.builder()
                .id(200)
                .uuid("reviewer-uuid")
                .name("审核管理员")
                .roles(List.of(RoleConstants.RESEARCH_ADMIN))
                .enabled(true)
                .build();
        AssignReviewerDTO dto = new AssignReviewerDTO();
        dto.setReviewerIds(List.of(200));
        dto.setReviewerNames(List.of("审核管理员"));

        when(achievementMainsMapper.selectOne(any())).thenReturn(achievement);
        when(keycloakUserService.getUserById(200)).thenReturn(reviewer);

        ReviewResultVO result = achievementReviewService.assignReviewers("ach-doc-1", dto, assigner);

        assertNotNull(result);
        assertTrue(Boolean.TRUE.equals(result.getSuccess()));
        assertEquals("ach-doc-1", result.getAchievementDocId());
        assertEquals(200, result.getReviewerId());
        assertEquals("审核管理员", result.getReviewerName());

        verify(achievementMainsMapper).update(eq(null), any());

        ArgumentCaptor<AchievementReviewerAssignment> assignmentCaptor =
                ArgumentCaptor.forClass(AchievementReviewerAssignment.class);
        verify(assignmentMapper).insert(assignmentCaptor.capture());
        AchievementReviewerAssignment assignment = assignmentCaptor.getValue();
        assertEquals(10, assignment.getAchievementId());
        assertEquals("ach-doc-1", assignment.getAchievementDocId());
        assertEquals(200, assignment.getReviewerId());
        assertEquals("审核管理员", assignment.getReviewerName());
        assertEquals("pending", assignment.getStatus());
    }

    @Test
    void assignReviewersShouldRejectNonReviewRolesBeforeWriting() {
        AchievementMains achievement = new AchievementMains()
                .setId(11)
                .setDocumentId("ach-doc-2")
                .setPublishedAt(LocalDateTime.now())
                .setIsDelete(0);
        KeycloakUser assigner = KeycloakUser.builder()
                .id(100)
                .uuid("assigner-uuid")
                .name("分配管理员")
                .roles(List.of(RoleConstants.RESEARCH_ADMIN))
                .enabled(true)
                .build();
        KeycloakUser invalidReviewer = KeycloakUser.builder()
                .id(300)
                .uuid("reviewer-uuid")
                .name("普通用户")
                .roles(List.of(RoleConstants.PROJECT_LEADER))
                .enabled(true)
                .build();
        AssignReviewerDTO dto = new AssignReviewerDTO();
        dto.setReviewerIds(List.of(300));

        when(achievementMainsMapper.selectOne(any())).thenReturn(achievement);
        when(keycloakUserService.getUserById(300)).thenReturn(invalidReviewer);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> achievementReviewService.assignReviewers("ach-doc-2", dto, assigner));

        assertTrue(exception.getMessage().contains("审核人必须是科研管理员或科研专家"));
        verify(achievementMainsMapper, never()).update(eq(null), any());
        verify(assignmentMapper, never()).insert(any());
    }

    @Test
    void assignReviewersShouldRejectDisabledReviewerBeforeWriting() {
        AchievementMains achievement = new AchievementMains()
                .setId(12)
                .setDocumentId("ach-doc-3")
                .setPublishedAt(LocalDateTime.now())
                .setIsDelete(0);
        KeycloakUser assigner = KeycloakUser.builder()
                .id(100)
                .uuid("assigner-uuid")
                .name("分配管理员")
                .roles(List.of(RoleConstants.RESEARCH_ADMIN))
                .enabled(true)
                .build();
        KeycloakUser disabledReviewer = KeycloakUser.builder()
                .id(400)
                .uuid("reviewer-uuid")
                .name("已禁用管理员")
                .roles(List.of(RoleConstants.RESEARCH_ADMIN))
                .enabled(false)
                .build();
        AssignReviewerDTO dto = new AssignReviewerDTO();
        dto.setReviewerIds(List.of(400));

        when(achievementMainsMapper.selectOne(any())).thenReturn(achievement);
        when(keycloakUserService.getUserById(400)).thenReturn(disabledReviewer);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> achievementReviewService.assignReviewers("ach-doc-3", dto, assigner));

        assertTrue(exception.getMessage().contains("审核人已被禁用"));
        verify(achievementMainsMapper, never()).update(eq(null), any());
        verify(assignmentMapper, never()).insert(any());
    }

    @Test
    void reviewAchievementShouldAutoIndexWhenApproved() {
        AchievementMains achievement = new AchievementMains()
                .setId(20)
                .setDocumentId("ach-doc-approved")
                .setAchievementStatus("UNDER_REVIEW")
                .setPublishedAt(LocalDateTime.now())
                .setIsDelete(0);
        KeycloakUser reviewer = KeycloakUser.builder()
                .id(200)
                .uuid("reviewer-uuid")
                .name("审核管理员")
                .roles(List.of(RoleConstants.RESEARCH_ADMIN))
                .enabled(true)
                .build();
        ReviewRequestDTO dto = new ReviewRequestDTO();
        dto.setAction("approve");
        dto.setComment("审核通过");

        RagIndexResultVO indexResult = new RagIndexResultVO();
        indexResult.setTotal(1);
        indexResult.setSuccess(1);

        when(achievementMainsMapper.selectOne(any())).thenReturn(achievement);
        when(ragAchievementIndexService.rebuildAndIndexAchievementDoc("ach-doc-approved")).thenReturn(indexResult);

        ReviewResultVO result = achievementReviewService.reviewAchievement("ach-doc-approved", dto, reviewer);

        assertNotNull(result);
        assertTrue(Boolean.TRUE.equals(result.getSuccess()));
        assertEquals("APPROVED", result.getStatus());
        verify(achievementMainsMapper).update(eq(null), any());
        verify(achievementReviewMapper).insert(any());
        verify(assignmentMapper).update(eq(null), any());
        verify(ragAchievementIndexService).rebuildAndIndexAchievementDoc("ach-doc-approved");
    }

    @Test
    void reviewAchievementShouldNotIndexWhenRejected() {
        AchievementMains achievement = new AchievementMains()
                .setId(21)
                .setDocumentId("ach-doc-rejected")
                .setAchievementStatus("UNDER_REVIEW")
                .setPublishedAt(LocalDateTime.now())
                .setIsDelete(0);
        KeycloakUser reviewer = KeycloakUser.builder()
                .id(200)
                .uuid("reviewer-uuid")
                .name("审核管理员")
                .roles(List.of(RoleConstants.RESEARCH_ADMIN))
                .enabled(true)
                .build();
        ReviewRequestDTO dto = new ReviewRequestDTO();
        dto.setAction("reject");
        dto.setComment("审核驳回");

        when(achievementMainsMapper.selectOne(any())).thenReturn(achievement);

        ReviewResultVO result = achievementReviewService.reviewAchievement("ach-doc-rejected", dto, reviewer);

        assertNotNull(result);
        assertTrue(Boolean.TRUE.equals(result.getSuccess()));
        assertEquals("REJECTED", result.getStatus());
        verify(ragAchievementIndexService, never()).rebuildAndIndexAchievementDoc(any());
    }
}
