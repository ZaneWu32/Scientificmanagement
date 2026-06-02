package com.achievement.service;

import java.util.List;

import com.achievement.domain.dto.DemandMatchPreviewRequest;
import com.achievement.domain.dto.DemandQueryDTO;
import com.achievement.domain.dto.DemandStatusUpdateDTO;
import com.achievement.domain.dto.KeycloakUser;
import com.achievement.domain.vo.DemandInsightStatsVO;
import com.achievement.domain.vo.DemandInsightVO;
import com.achievement.domain.vo.DemandMatchVO;
import com.achievement.domain.vo.DemandSourceVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface IDemandInsightService {

    DemandInsightStatsVO getStats();

    Page<DemandInsightVO> pageList(DemandQueryDTO query);

    DemandInsightVO detail(Long id);

    List<DemandMatchVO> rematch(Long demandId);

    DemandInsightVO updateStatus(Long demandId, DemandStatusUpdateDTO dto, KeycloakUser currentUser);

    DemandInsightVO confirmMatch(Long demandId, String resultId, KeycloakUser currentUser);

    DemandInsightVO rejectMatch(Long demandId, String resultId, KeycloakUser currentUser);

    List<DemandSourceVO> listSources();

    List<DemandMatchVO> previewMatch(DemandMatchPreviewRequest request);
}
