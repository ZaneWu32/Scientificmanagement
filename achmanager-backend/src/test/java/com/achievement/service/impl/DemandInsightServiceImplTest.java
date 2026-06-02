package com.achievement.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.achievement.domain.dto.KeycloakUser;
import com.achievement.domain.po.DemandItem;
import com.achievement.domain.po.DemandMatch;
import com.achievement.domain.vo.AchievementSearchHitVO;
import com.achievement.domain.vo.DemandInsightVO;
import com.achievement.domain.vo.DemandMatchVO;
import com.achievement.mapper.DemandFollowUpMapper;
import com.achievement.mapper.DemandItemMapper;
import com.achievement.mapper.DemandMatchMapper;
import com.achievement.mapper.DemandSourceMapper;
import com.achievement.service.IDemandMatchLlmRerankService;
import com.achievement.service.IRagAchievementIndexService;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class DemandInsightServiceImplTest {

    @Mock
    private DemandItemMapper demandItemMapper;

    @Mock
    private DemandMatchMapper demandMatchMapper;

    @Mock
    private DemandSourceMapper demandSourceMapper;

    @Mock
    private DemandFollowUpMapper demandFollowUpMapper;

    @Mock
    private IRagAchievementIndexService ragAchievementIndexService;

    @Mock
    private IDemandMatchLlmRerankService demandMatchLlmRerankService;

    private DemandInsightServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DemandInsightServiceImpl(
                demandItemMapper,
                demandMatchMapper,
                demandSourceMapper,
                demandFollowUpMapper,
                ragAchievementIndexService,
                demandMatchLlmRerankService,
                new ObjectMapper());
    }

    @Test
    void rematchShouldSearchRagPersistMatchesAndReturnDisplayFields() {
        DemandItem demand = new DemandItem()
                .setId(42L)
                .setTitle("无人机巡检需求")
                .setRawContent("希望基于上传材料中的线路图像完成缺陷识别和巡检调度。")
                .setSummary("电力线路巡检和缺陷识别需求")
                .setLlmSummary("寻找无人机电力巡检成果")
                .setIndustry("电力")
                .setRegion("浙江")
                .setKeywordsJson("[\"无人机\",\"巡检\"]")
                .setTagsJson("[\"缺陷识别\"]")
                .setStatus("new")
                .setIsDelete(0);
        when(demandItemMapper.selectById(42L)).thenReturn(demand);

        AchievementSearchHitVO primary = hit(
                "ach-doc-a",
                "无人机电力巡检成果",
                "技术成果",
                "低空智能巡检",
                "无人机,巡检,缺陷识别",
                "张三,李四",
                "proposal.pdf",
                "附件显示该成果支持输电线路缺陷识别。",
                8D);
        AchievementSearchHitVO secondary = hit(
                "ach-doc-b",
                "变电站图像识别平台",
                "软件著作权",
                "视觉算法平台",
                "图像识别,巡检",
                "王五",
                "",
                "摘要提到巡检图像分类。",
                4D);
        AchievementSearchHitVO invalid = hit(
                "",
                "缺少文档 ID 的命中",
                "技术成果",
                "",
                "",
                "",
                "",
                "不应入库。",
                3D);
        when(ragAchievementIndexService.searchAchievements(anyString(), eq(10)))
                .thenReturn(List.of(primary, secondary, invalid));
        when(demandMatchLlmRerankService.rerank(eq(demand), anyList(), eq(8D)))
                .thenAnswer(invocation -> {
                    List<DemandMatch> candidates = new ArrayList<>(invocation.getArgument(1));
                    candidates.sort(Comparator.comparing(DemandMatch::getMatchScore).reversed());
                    return candidates;
                });
        when(demandMatchMapper.insert(any(DemandMatch.class))).thenAnswer(invocation -> {
            DemandMatch match = invocation.getArgument(0);
            match.setId("ach-doc-a".equals(match.getAchievementDocId()) ? 501L : 502L);
            return 1;
        });

        List<DemandMatchVO> result = service.rematch(42L);

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(ragAchievementIndexService).searchAchievements(queryCaptor.capture(), eq(10));
        String query = queryCaptor.getValue();
        assertTrue(query.contains("无人机巡检需求"));
        assertTrue(query.contains("寻找无人机电力巡检成果"));
        assertTrue(query.contains("缺陷识别"));

        verify(demandMatchMapper).softDeleteUnconfirmedByDemandId(42L);
        ArgumentCaptor<DemandMatch> matchCaptor = ArgumentCaptor.forClass(DemandMatch.class);
        verify(demandMatchMapper, times(2)).insert(matchCaptor.capture());
        List<DemandMatch> inserted = matchCaptor.getAllValues();
        DemandMatch first = inserted.get(0);
        assertEquals(42L, first.getDemandId());
        assertEquals("ach-doc-a", first.getAchievementDocId());
        assertEquals("无人机电力巡检成果", first.getResultTitle());
        assertEquals("张三,李四", first.getOwner());
        assertEquals(new BigDecimal("8.0000"), first.getEsScore());
        assertTrue(first.getRuleScore().compareTo(new BigDecimal("0.600")) >= 0);
        assertTrue(first.getReason().contains("无人机"));
        assertTrue(first.getFitTagsJson().contains("含附件预览"));
        assertEquals("pending", first.getConfirmStatus());

        ArgumentCaptor<DemandItem> updateCaptor = ArgumentCaptor.forClass(DemandItem.class);
        verify(demandItemMapper).updateById(updateCaptor.capture());
        DemandItem update = updateCaptor.getValue();
        assertEquals(42L, update.getId());
        assertEquals("matched", update.getStatus());
        assertEquals(first.getMatchScore(), update.getBestMatchScore());

        assertEquals(2, result.size());
        DemandMatchVO firstVo = result.get(0);
        assertEquals("501", firstVo.getMatchId());
        assertEquals("ach-doc-a", firstVo.getResultId());
        assertEquals("无人机电力巡检成果", firstVo.getResultTitle());
        assertEquals("技术成果", firstVo.getResultType());
        assertEquals("低空智能巡检", firstVo.getDepartment());
        assertEquals("附件显示该成果支持输电线路缺陷识别。", firstVo.getSourceSnippet());
        assertTrue(firstVo.getFitTags().contains("ES召回"));
        assertTrue(firstVo.getFitTags().contains("关键词命中"));
        assertTrue(firstVo.getFitTags().contains("证据片段"));
        assertTrue(firstVo.getFitTags().contains("含附件预览"));
        assertEquals("pending", firstVo.getConfirmStatus());
    }

    @Test
    void rematchShouldMoveDemandToReviewingWhenNoValidHitExists() {
        DemandItem demand = new DemandItem()
                .setId(43L)
                .setTitle("暂无成果需求")
                .setKeywordsJson("[]")
                .setTagsJson("[]")
                .setIsDelete(0);
        when(demandItemMapper.selectById(43L)).thenReturn(demand);
        when(ragAchievementIndexService.searchAchievements(anyString(), eq(10)))
                .thenReturn(List.of(hit(null, "空命中", "", "", "", "", "", "", 1D)));
        when(demandMatchLlmRerankService.rerank(eq(demand), anyList(), eq(1D)))
                .thenReturn(List.of());

        List<DemandMatchVO> result = service.rematch(43L);

        assertTrue(result.isEmpty());
        ArgumentCaptor<DemandItem> updateCaptor = ArgumentCaptor.forClass(DemandItem.class);
        verify(demandItemMapper).updateById(updateCaptor.capture());
        DemandItem update = updateCaptor.getValue();
        assertEquals("reviewing", update.getStatus());
        assertEquals(new BigDecimal("0.000"), update.getBestMatchScore());
    }

    @Test
    void confirmMatchShouldMarkCandidateAndMoveDemandToFollowUp() {
        DemandItem existingDemand = new DemandItem()
                .setId(42L)
                .setTitle("无人机巡检需求")
                .setStatus("matched")
                .setKeywordsJson("[]")
                .setTagsJson("[]")
                .setIsDelete(0);
        DemandItem updatedDemand = new DemandItem()
                .setId(42L)
                .setTitle("无人机巡检需求")
                .setStatus("in_follow_up")
                .setBestMatchScore(new BigDecimal("0.910"))
                .setKeywordsJson("[]")
                .setTagsJson("[]")
                .setIsDelete(0);
        DemandMatch target = new DemandMatch()
                .setId(501L)
                .setDemandId(42L)
                .setAchievementDocId("ach-doc-a")
                .setResultTitle("无人机电力巡检成果")
                .setMatchScore(new BigDecimal("0.910"))
                .setConfirmStatus("pending")
                .setFitTagsJson("[]")
                .setIsDelete(0);
        when(demandItemMapper.selectById(42L)).thenReturn(existingDemand, updatedDemand);
        when(demandMatchMapper.selectActiveByDemandId(42L)).thenReturn(List.of(target));
        when(demandFollowUpMapper.selectActiveByDemandId(42L)).thenReturn(List.of());

        DemandInsightVO result = service.confirmMatch(
                42L,
                "ach-doc-a",
                KeycloakUser.builder().username("research_admin").name("审核管理员").build());

        ArgumentCaptor<DemandMatch> matchCaptor = ArgumentCaptor.forClass(DemandMatch.class);
        verify(demandMatchMapper).updateById(matchCaptor.capture());
        DemandMatch updatedMatch = matchCaptor.getValue();
        assertEquals("confirmed", updatedMatch.getConfirmStatus());
        assertEquals("research_admin", updatedMatch.getConfirmedBy());
        assertNotNull(updatedMatch.getConfirmedAt());
        assertNotNull(updatedMatch.getUpdatedAt());

        ArgumentCaptor<DemandItem> demandCaptor = ArgumentCaptor.forClass(DemandItem.class);
        verify(demandItemMapper).updateById(demandCaptor.capture());
        DemandItem demandUpdate = demandCaptor.getValue();
        assertEquals(42L, demandUpdate.getId());
        assertEquals("in_follow_up", demandUpdate.getStatus());
        assertEquals(new BigDecimal("0.910"), demandUpdate.getBestMatchScore());

        assertEquals("in_follow_up", result.getStatus());
        assertEquals(new BigDecimal("0.910"), result.getBestMatchScore());
        assertEquals(1, result.getMatches().size());
        assertEquals("confirmed", result.getMatches().get(0).getConfirmStatus());
    }

    private AchievementSearchHitVO hit(String achievementDocId,
                                       String title,
                                       String typeName,
                                       String projectName,
                                       String keywordsText,
                                       String authorsText,
                                       String attachmentNames,
                                       String sourceSnippet,
                                       Double esScore) {
        AchievementSearchHitVO hit = new AchievementSearchHitVO();
        hit.setAchievementDocId(achievementDocId);
        hit.setTitle(title);
        hit.setTypeName(typeName);
        hit.setProjectName(projectName);
        hit.setKeywordsText(keywordsText);
        hit.setAuthorsText(authorsText);
        hit.setAttachmentNames(attachmentNames);
        hit.setSourceSnippet(sourceSnippet);
        hit.setEsScore(esScore);
        return hit;
    }
}
