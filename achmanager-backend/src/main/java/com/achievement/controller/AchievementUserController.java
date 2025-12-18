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

//12.17 目前开发成果物有关的查询功能
@Slf4j
@RestController
@RequestMapping("/user/achievement")
@RequiredArgsConstructor
@Tag(name="用户成果物管理相关接口")
public class AchievementUserController {
    private final IAchievementMainsService achievementMainsService;

    /* 用户分页查询个人成果物列表
    *
    * */
    @Operation(description = "用户分页查询成果物列表接口")
    @PostMapping("/pageList")
    public Result<Page<AchListVO>> pageList(@RequestBody AchListDTO achListDTO){
        return Result.success(achievementMainsService.pageList4User(achListDTO));
    }
}
