package com.achievement.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.achievement.result.Result;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/health")
public class HealthController {
    @GetMapping
    public Result<String> health() {
        log.debug("收到健康检查请求");
        return Result.success("系统运行正常");
    }
}
