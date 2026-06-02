package com.achievement.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.achievement.annotation.CurrentUser;
import com.achievement.domain.dto.DemandConfirmMatchDTO;
import com.achievement.domain.dto.DemandMatchPreviewRequest;
import com.achievement.domain.dto.DemandQueryDTO;
import com.achievement.domain.dto.DemandStatusUpdateDTO;
import com.achievement.domain.dto.KeycloakUser;
import com.achievement.domain.vo.DemandInsightStatsVO;
import com.achievement.domain.vo.DemandInsightVO;
import com.achievement.domain.vo.DemandMatchVO;
import com.achievement.domain.vo.DemandSourceVO;
import com.achievement.result.Result;
import com.achievement.service.IDemandInsightService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/demand")
@RequiredArgsConstructor
@Tag(name = "需求洞察接口")
public class DemandInsightController {

    private final IDemandInsightService demandInsightService;

    @Operation(description = "需求洞察指标统计")
    @GetMapping("/statistics")
    public Result<DemandInsightStatsVO> getStats() {
        return Result.success(demandInsightService.getStats());
    }

    @Operation(description = "需求池分页列表")
    @GetMapping
    public Result<Page<DemandInsightVO>> list(@RequestParam(required = false) Integer pageNum,
                                              @RequestParam(required = false) Integer pageSize,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(required = false) String industry,
                                              @RequestParam(required = false) String region) {
        DemandQueryDTO query = new DemandQueryDTO();
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        query.setKeyword(keyword);
        query.setStatus(status);
        query.setIndustry(industry);
        query.setRegion(region);
        return Result.success(demandInsightService.pageList(query));
    }

    @Operation(description = "需求来源列表")
    @GetMapping("/sources")
    public Result<List<DemandSourceVO>> sources() {
        return Result.success(demandInsightService.listSources());
    }

    @Operation(description = "临时输入需求文本，预览 ES 成果匹配结果，不入库")
    @PostMapping("/match-preview")
    public Result<List<DemandMatchVO>> previewMatch(@RequestBody DemandMatchPreviewRequest request) {
        return Result.success(demandInsightService.previewMatch(request));
    }

    @Operation(description = "需求详情")
    @GetMapping("/{id}")
    public Result<DemandInsightVO> detail(@PathVariable Long id) {
        return Result.success(demandInsightService.detail(id));
    }

    @Operation(description = "重新计算需求与成果的匹配结果")
    @PostMapping("/{id}/rematch")
    public Result<List<DemandMatchVO>> rematch(@PathVariable Long id) {
        return Result.success(demandInsightService.rematch(id));
    }

    @Operation(description = "更新需求跟进状态")
    @PatchMapping("/{id}/status")
    public Result<DemandInsightVO> updateStatus(@PathVariable Long id,
                                                @RequestBody DemandStatusUpdateDTO dto,
                                                @CurrentUser KeycloakUser currentUser) {
        return Result.success(demandInsightService.updateStatus(id, dto, currentUser));
    }

    @Operation(description = "人工确认候选成果匹配结果")
    @PostMapping("/{id}/confirm-match")
    public Result<DemandInsightVO> confirmMatch(@PathVariable Long id,
                                                @RequestBody DemandConfirmMatchDTO dto,
                                                @CurrentUser KeycloakUser currentUser) {
        return Result.success(demandInsightService.confirmMatch(id, dto.getResultId(), currentUser));
    }

    @Operation(description = "人工排除不合适的候选成果")
    @PostMapping("/{id}/reject-match")
    public Result<DemandInsightVO> rejectMatch(@PathVariable Long id,
                                               @RequestBody DemandConfirmMatchDTO dto,
                                               @CurrentUser KeycloakUser currentUser) {
        return Result.success(demandInsightService.rejectMatch(id, dto.getResultId(), currentUser));
    }
}
