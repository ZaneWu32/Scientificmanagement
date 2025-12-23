package com.achievement.controller;

import com.achievement.service.IAchievementFieldValuesService;
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
@RequestMapping("/achievementFieldValue")
@RequiredArgsConstructor
@Tag(name = "成果物字段值相关接口")
public class AchievementFieldValueController {

    private final IAchievementFieldValuesService fieldValuesService;

    @PostMapping("/values")
    @Operation(description = "新增字段值")
    public JsonNode createFieldValue(@RequestBody Map<String, Object> req) {
        log.info("新增字段值");
        return fieldValuesService.createFieldValue(req);
    }

    @PutMapping("/values/{fieldValueDocId}")
    @Operation(description = "更新字段值")
    public JsonNode updateFieldValue(@PathVariable String fieldValueDocId, @RequestBody Map<String, Object> req) {
        log.info("更新字段值 fieldValueDocId={}", fieldValueDocId);
        return fieldValuesService.updateFieldValue(fieldValueDocId, req);
    }
}
