package com.achievement.controller.stat;

import com.achievement.result.Result;
import com.achievement.service.IAchievementMainsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/admin/stat/")
@Tag(name = "管理员统计成果物")
@RequiredArgsConstructor
public class AdminStatController {

    private final IAchievementMainsService mainsService;

        //统计某用户的成果物数量
        @Operation(description = "统计所有用户的成果物数量")
        @PostMapping("/AchCount")
        public Result<Long> AchStat(){
            Long count = mainsService.countAch();
            return Result.success(count);

        }
        //统计某用户的某类别成果物数量
        @Operation(description = "统计创建的某类别成果物数量")
        @PostMapping("/TypeAchCount")
        public Result<Long> TypeAchStat(@RequestParam Long typeId){

            Long count = mainsService.countByTypeId(typeId);
            return Result.success(count);

        }
        @Operation(description = "统计系统本月新增成果物数量")
        //统计某用户的本月新增成果物数量
        @PostMapping("/MonthNewAchCount")
        public Result<Long> MonthNewAchStat(){
            Long count = mainsService.countMonthNew();
            return Result.success(count);
        }
    }
