package com.achievement.controller;

import com.achievement.service.IAchievementFieldDefsService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/achievementFieldDef")
@RequiredArgsConstructor
@Tag(name = "成果物字段定义相关接口")
public class AchievementFieldDefController {

    private final IAchievementFieldDefsService fieldDefsService;

    @PostMapping("/defs")
    @Operation(description = "新增字段定义")
    public JsonNode createFieldDef(@RequestBody Map<String, Object> req) {
        log.info("新增字段定义");
        return fieldDefsService.createFieldDef(req);
    }

    @PutMapping("/defs/{fieldDefDocId}")
    @Operation(description = "更新字段定义")
    public JsonNode updateFieldDef(@PathVariable String fieldDefDocId, @RequestBody Map<String, Object> req) {
        log.info("更新字段定义 fieldDefDocId={}", fieldDefDocId);
        return fieldDefsService.updateFieldDef(fieldDefDocId, req);
    }
}
