package com.achievement.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.achievement.client.StrapiClient;
import com.achievement.domain.vo.RagIndexResultVO;
import com.achievement.service.IRagAchievementIndexService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class AchievementAdminServiceImplTest {

    @Mock
    private StrapiClient strapiClient;

    @Mock
    private IRagAchievementIndexService ragAchievementIndexService;

    private AchievementAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AchievementAdminServiceImpl(strapiClient, new ObjectMapper(), ragAchievementIndexService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void createAchievementWithFilesShouldBindUploadedFilesAndSyncRag() {
        Map<String, Object> req = requestWithData(Map.of(
                "title", "无人机电力巡检成果",
                "projectName", "低空智能巡检",
                "visibilityRange", "public"));
        MultipartFile file = new MockMultipartFile(
                "files",
                "proposal.pdf",
                "application/pdf",
                "proposal content".getBytes(StandardCharsets.UTF_8));

        when(strapiClient.upload(any(MultipartFile[].class)))
                .thenReturn("[{\"id\":11},{\"id\":12}]");
        when(strapiClient.create(eq("achievement-mains"), anyMap()))
                .thenReturn("{\"data\":{\"documentId\":\"ach-doc-new\"}}");
        when(strapiClient.create(eq("achievement-files"), anyMap()))
                .thenReturn("{\"data\":{\"documentId\":\"attach-doc-new\"}}");
        when(ragAchievementIndexService.syncAchievementDoc("ach-doc-new")).thenReturn(successResult());

        JsonNode result = service.createAchievementWithFiles(req, new MultipartFile[]{file}, 99);

        ArgumentCaptor<Map<String, Object>> mainCaptor = ArgumentCaptor.forClass(Map.class);
        verify(strapiClient).create(eq("achievement-mains"), mainCaptor.capture());
        Map<String, Object> mainData = (Map<String, Object>) mainCaptor.getValue().get("data");
        assertEquals("无人机电力巡检成果", mainData.get("title"));
        assertEquals("低空智能巡检", mainData.get("project_name"));
        assertEquals("public", mainData.get("visibility_range"));
        assertEquals("99", mainData.get("created_by_user_id"));
        assertFalse(mainData.containsKey("attachments"));

        ArgumentCaptor<Map<String, Object>> attachmentCaptor = ArgumentCaptor.forClass(Map.class);
        verify(strapiClient).create(eq("achievement-files"), attachmentCaptor.capture());
        Map<String, Object> attachmentData = (Map<String, Object>) attachmentCaptor.getValue().get("data");
        assertEquals("ach-doc-new", attachmentData.get("achievement_main_id"));
        assertEquals(List.of(11, 12), attachmentData.get("files"));
        assertEquals(0, attachmentData.get("is_delete"));

        verify(ragAchievementIndexService).syncAchievementDoc("ach-doc-new");
        assertTrue(result.path("achievement").path("data").path("documentId").asText().equals("ach-doc-new"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void updateAchievementWithFilesKeepStatusShouldMergeExistingAndUploadedFilesThenSyncRag() {
        Map<String, Object> req = requestWithData(new HashMap<>(Map.of(
                "title", "无人机电力巡检成果升级版",
                "attachments", List.of(Map.of("data", Map.of(
                        "files", List.of(1, Map.of("id", 2), Map.of("data", List.of(3)))))))));
        MultipartFile file = new MockMultipartFile(
                "files",
                "updated-proposal.pdf",
                "application/pdf",
                "updated content".getBytes(StandardCharsets.UTF_8));

        when(strapiClient.upload(any(MultipartFile[].class)))
                .thenReturn("[{\"id\":4},{\"id\":5}]");
        when(strapiClient.update(eq("achievement-mains"), eq("ach-doc-1"), anyMap()))
                .thenReturn("{\"data\":{\"documentId\":\"ach-doc-1\"}}");
        when(strapiClient.query(eq("achievement-files"), anyMap()))
                .thenReturn("{\"data\":[{\"documentId\":\"old-attach-1\"},{\"attributes\":{\"documentId\":\"old-attach-2\"}}]}",
                        "{\"data\":[]}");
        when(strapiClient.update(eq("achievement-files"), any(), anyMap()))
                .thenReturn("{\"data\":{\"documentId\":\"old-attach-updated\"}}");
        when(strapiClient.create(eq("achievement-files"), anyMap()))
                .thenReturn("{\"data\":{\"documentId\":\"attach-doc-new\"}}");
        when(ragAchievementIndexService.syncAchievementDoc("ach-doc-1")).thenReturn(successResult());

        JsonNode result = service.updateAchievementWithFilesKeepStatus("ach-doc-1", req, new MultipartFile[]{file});

        ArgumentCaptor<Map<String, Object>> mainCaptor = ArgumentCaptor.forClass(Map.class);
        verify(strapiClient).update(eq("achievement-mains"), eq("ach-doc-1"), mainCaptor.capture());
        Map<String, Object> mainData = (Map<String, Object>) mainCaptor.getValue().get("data");
        assertEquals("无人机电力巡检成果升级版", mainData.get("title"));
        assertFalse(mainData.containsKey("attachments"));
        assertFalse(mainData.containsKey("achievement_status"));

        ArgumentCaptor<Object> deletedAttachmentDocIdCaptor = ArgumentCaptor.forClass(Object.class);
        verify(strapiClient, times(2)).update(
                eq("achievement-files"),
                deletedAttachmentDocIdCaptor.capture(),
                anyMap());
        assertEquals(List.of("old-attach-1", "old-attach-2"), deletedAttachmentDocIdCaptor.getAllValues());

        ArgumentCaptor<Map<String, Object>> attachmentCaptor = ArgumentCaptor.forClass(Map.class);
        verify(strapiClient).create(eq("achievement-files"), attachmentCaptor.capture());
        Map<String, Object> attachmentData = (Map<String, Object>) attachmentCaptor.getValue().get("data");
        assertEquals("ach-doc-1", attachmentData.get("achievement_main_id"));
        assertEquals(List.of(1, 2, 3, 4, 5), attachmentData.get("files"));
        assertEquals(0, attachmentData.get("is_delete"));

        verify(ragAchievementIndexService).syncAchievementDoc("ach-doc-1");
        assertTrue(result.has("attachments"));
    }

    @Test
    void createAchievementWithFilesShouldFailWhenUploadReturnsNoFileIds() {
        Map<String, Object> req = requestWithData(Map.of("title", "无人机电力巡检成果"));
        MultipartFile file = new MockMultipartFile(
                "files",
                "proposal.pdf",
                "application/pdf",
                "proposal content".getBytes(StandardCharsets.UTF_8));
        when(strapiClient.upload(any(MultipartFile[].class))).thenReturn("[]");

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> service.createAchievementWithFiles(req, new MultipartFile[]{file}, 99));

        assertTrue(exception.getMessage().contains("文件上传失败"));
        verify(strapiClient, never()).create(eq("achievement-mains"), anyMap());
        verify(ragAchievementIndexService, never()).syncAchievementDoc(any());
    }

    @Test
    void updateAchievementWithFilesShouldFailBeforeRagSyncWhenAttachmentReplaceFails() {
        Map<String, Object> req = requestWithData(Map.of("title", "无人机电力巡检成果升级版"));
        MultipartFile file = new MockMultipartFile(
                "files",
                "updated-proposal.pdf",
                "application/pdf",
                "updated content".getBytes(StandardCharsets.UTF_8));

        when(strapiClient.upload(any(MultipartFile[].class))).thenReturn("[{\"id\":4}]");
        when(strapiClient.update(eq("achievement-mains"), eq("ach-doc-1"), anyMap()))
                .thenReturn("{\"data\":{\"documentId\":\"ach-doc-1\"}}");
        when(strapiClient.query(eq("achievement-files"), anyMap()))
                .thenReturn("{\"data\":[{\"documentId\":\"old-attach-1\"}]}");
        when(strapiClient.update(eq("achievement-files"), eq("old-attach-1"), anyMap()))
                .thenThrow(new RuntimeException("Strapi unavailable"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> service.updateAchievementWithFilesKeepStatus("ach-doc-1", req, new MultipartFile[]{file}));

        assertTrue(exception.getMessage().contains("覆盖附件失败"));
        verify(strapiClient, never()).create(eq("achievement-files"), anyMap());
        verify(ragAchievementIndexService, never()).syncAchievementDoc(any());
    }

    private Map<String, Object> requestWithData(Map<String, Object> data) {
        Map<String, Object> req = new HashMap<>();
        req.put("data", new HashMap<>(data));
        return req;
    }

    private RagIndexResultVO successResult() {
        RagIndexResultVO result = new RagIndexResultVO();
        result.setTotal(1);
        result.setSuccess(1);
        return result;
    }
}
