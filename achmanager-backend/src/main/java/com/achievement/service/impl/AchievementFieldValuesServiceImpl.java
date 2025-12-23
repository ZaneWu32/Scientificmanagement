package com.achievement.service.impl;

import com.achievement.client.StrapiClient;
import com.achievement.domain.po.AchievementFieldValues;
import com.achievement.mapper.AchievementFieldValuesMapper;
import com.achievement.service.IAchievementFieldValuesService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author author
 * @since 2025-12-11
 */
@Service
@RequiredArgsConstructor
public class AchievementFieldValuesServiceImpl extends ServiceImpl<AchievementFieldValuesMapper, AchievementFieldValues> implements IAchievementFieldValuesService {

    private final StrapiClient strapiClient;

    @Override
    public JsonNode createFieldValue(Map<String, Object> req) {
        String raw = strapiClient.create("achievement-field-values", req);
        try {
            return new ObjectMapper().readTree(raw);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JsonNode updateFieldValue(String fieldValueDocId, Map<String, Object> req) {
        String raw = strapiClient.update("achievement-field-values", fieldValueDocId, req);
        try {
            return new ObjectMapper().readTree(raw);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
