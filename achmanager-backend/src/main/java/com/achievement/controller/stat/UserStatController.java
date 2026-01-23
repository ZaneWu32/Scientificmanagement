package com.achievement.controller.stat;

import com.achievement.annotation.CurrentUser;
import com.achievement.domain.po.BusinessUser;
import com.achievement.domain.vo.TypeCountVO;
import com.achievement.result.Result;
import com.achievement.service.AchievementStatService;
import com.achievement.service.IAchievementMainsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//12.14 统计某用户的成果物数量
@Slf4j
@RestController
@RequestMapping("/user/stat/")
@RequiredArgsConstructor
@Tag(name="统计用户的成果物")
public class UserStatController {
    private final IAchievementMainsService mainsService;
    private final AchievementStatService statService;
    //统计某用户的成果物数量
    @Operation(description = "统计某用户的成果物数量")
    @GetMapping("/userAchCount")
    public Result<Long> userAchStat(@CurrentUser BusinessUser currentUser){
        if (currentUser == null) {
            return Result.error(401, "未登录");
        }
        Long count = mainsService.countByUserId(Long.valueOf(currentUser.getId()));
        return Result.success(count);

    }
    //统计某用户的某类别成果物数量
    @Operation(description = "统计某用户创建的某类别成果物数量")
    @GetMapping("/userTypeAchCount")
    public Result<Long> userTypeAchStat(@RequestParam Long typeId, @CurrentUser BusinessUser currentUser){
        if (currentUser == null) {
            return Result.error(401, "未登录");
        }
        Long count = mainsService.countByUserIdAndTypeId(typeId, Long.valueOf(currentUser.getId()));
        return Result.success(count);

    }
    @Operation(description = "统计某用户的本月新增成果物数量")
    //统计某用户的本月新增成果物数量
    @GetMapping("/userMonthNewAchCount")
    public Result<Long> userMonthNewAchStat(@CurrentUser BusinessUser currentUser){
        if (currentUser == null) {
            return Result.error(401, "未登录");
        }
        Long count = mainsService.countMonthNewByUserId(Long.valueOf(currentUser.getId()));
        return Result.success(count);
    }
    @Operation(description = "饼图：各成果物类型数量")
    @GetMapping("/typePie")
    public Result<List<TypeCountVO>> typePie(@CurrentUser BusinessUser businessUser) {
        if (businessUser == null) {
            return Result.error("未登录");
        }
        return Result.success(statService.typePie(Long.valueOf(businessUser.getId())));
    }


}
