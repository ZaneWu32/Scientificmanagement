package com.achievement.controller;


import com.achievement.domain.dto.AchListDTO;
import com.achievement.domain.vo.AchListVO;
import com.achievement.result.Result;
import com.achievement.service.IAchievementMainsService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author author
 * @since 2025-12-11
 */
//12.11 目前开发成果物有关的查询功能
@Slf4j
@RestController
@RequestMapping("/admin/achievement")
@RequiredArgsConstructor
@Tag(name="管理员成果物管理相关接口")
public class AchievementManageController {
    private final IAchievementMainsService achievementMainsService;
    /* 管理员分页查询所有成果物列表
     */
    @Operation(description = "管理员分页查询成果物列表接口")
    @PostMapping("/pageList")
    public Result<Page<AchListVO>> pageList(@RequestBody AchListDTO achListDTO){
        return Result.success(achievementMainsService.pageList(achListDTO));
    }
    /*
     *TODO管理员查询成果物详情
     * */
    /*
     *TODO管理员新增成果物
     * */
    /*
     *TODO管理员修改成果物
     * */
    /*
     * TODO管理员审核成果物
     * */


}
