package com.achievement.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.achievement.client.RagElasticsearchClient;
import com.achievement.domain.po.AchievementSearchDoc;
import com.achievement.domain.vo.AchDetailVO;
import com.achievement.domain.vo.RagIndexResultVO;
import com.achievement.mapper.AchievementSearchDocMapper;
import com.achievement.service.IAchievementMainsService;
import com.achievement.utils.AttachmentContentExtractor;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class RagAchievementIndexServiceImplTest {

    @Mock
    private IAchievementMainsService achievementMainsService;

    @Mock
    private AchievementSearchDocMapper achievementSearchDocMapper;

    @Mock
    private AttachmentContentExtractor attachmentContentExtractor;

    @Mock
    private RagElasticsearchClient ragElasticsearchClient;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private RagAchievementIndexServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RagAchievementIndexServiceImpl(
                achievementMainsService,
                achievementSearchDocMapper,
                attachmentContentExtractor,
                ragElasticsearchClient,
                objectMapper);
    }

    @Test
    void syncAchievementDocShouldRebuildAndIndexApprovedAchievement() throws Exception {
        AchDetailVO detail = new AchDetailVO();
        detail.setDocumentId("ach-doc-1");
        detail.setTitle("无人机电力巡检成果");
        detail.setSummary("面向输电线路的缺陷识别和巡检调度。");
        detail.setTypeName("技术成果");
        detail.setTypeCode("tech");
        detail.setProjectName("低空智能巡检");
        detail.setYear("2026");
        detail.setVisibilityRange("public");
        detail.setKeywords(List.of("无人机", "巡检", "缺陷识别"));
        detail.setAuthors(List.of("张三", "李四"));
        detail.setAttachments(objectMapper.readTree("""
                {
                  "data": [
                    {
                      "attributes": {
                        "files": {
                          "data": [
                            {
                              "attributes": {
                                "name": "proposal.pdf",
                                "url": "/uploads/proposal.pdf",
                                "mime": "application/pdf"
                              }
                            }
                          ]
                        }
                      }
                    }
                  ]
                }
                """));

        when(achievementSearchDocMapper.selectApprovedAchievementDocId("ach-doc-1"))
                .thenReturn("ach-doc-1");
        when(achievementMainsService.selectDetailForProjectSystem("ach-doc-1"))
                .thenReturn(detail);
        when(attachmentContentExtractor.extractContents(detail.getAttachments()))
                .thenReturn(Map.of("proposal.pdf", "附件正文包含无人机航线规划和缺陷检测算法。"));

        RagIndexResultVO result = service.syncAchievementDoc("ach-doc-1");

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getSuccess());
        assertEquals(0, result.getFailed());

        ArgumentCaptor<AchievementSearchDoc> docCaptor =
                ArgumentCaptor.forClass(AchievementSearchDoc.class);
        verify(achievementSearchDocMapper).insert(docCaptor.capture());
        AchievementSearchDoc doc = docCaptor.getValue();
        assertEquals("ach-doc-1", doc.getAchievementDocId());
        assertEquals("proposal.pdf", doc.getAttachmentNames());
        assertEquals("success", doc.getAttachmentExtractStatus());
        assertTrue(doc.getSearchText().contains("无人机电力巡检成果"));
        assertTrue(doc.getSearchText().contains("附件正文包含无人机航线规划"));

        verify(ragElasticsearchClient).ensureAchievementIndex();
        verify(ragElasticsearchClient).indexAchievement(doc);
        verify(achievementSearchDocMapper).markEsIndexed(
                isNull(), eq("ach-doc-1"), any(LocalDateTime.class));
    }

    @Test
    void syncAchievementDocShouldRemoveIndexWhenAchievementIsNotApproved() {
        when(achievementSearchDocMapper.selectApprovedAchievementDocId("ach-doc-archived"))
                .thenReturn(null);

        RagIndexResultVO result = service.syncAchievementDoc("ach-doc-archived");

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getSuccess());
        assertEquals(0, result.getFailed());
        verify(achievementSearchDocMapper).markDeletedByAchievementDocId(
                eq("ach-doc-archived"), any(LocalDateTime.class));
        verify(ragElasticsearchClient).deleteAchievement("ach-doc-archived");
        verifyNoInteractions(achievementMainsService, attachmentContentExtractor);
    }
}
