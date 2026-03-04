package com.achievement.controller;

import com.achievement.annotation.CurrentUser;
import com.achievement.domain.dto.AccessRequestCreateDTO;
import com.achievement.domain.dto.KeycloakUser;
import com.achievement.domain.vo.AccessRequestVO;
import com.achievement.result.Result;
import com.achievement.service.IAccessRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 成果访问申请创建接口
 */
@Slf4j
@RestController
@RequestMapping("/results")
@RequiredArgsConstructor
@Tag(name = "成果访问申请创建接口")
public class ResultAccessRequestApplyController {

    private final IAccessRequestService accessRequestService;

    @Operation(description = "提交成果全文访问申请")
    @PostMapping("/{id}/access-requests")
    public Result<AccessRequestVO> createAccessRequest(
            @PathVariable("id") String achievementDocId,
            @Valid @RequestBody AccessRequestCreateDTO createDTO,
            @CurrentUser KeycloakUser currentUser) {

        AccessRequestVO result = accessRequestService.createAccessRequest(
                achievementDocId, createDTO, currentUser);
        return Result.success(result);
    }
}
