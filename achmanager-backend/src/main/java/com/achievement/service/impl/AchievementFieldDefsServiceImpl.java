package com.achievement.service.impl;

import com.achievement.client.StrapiClient;
import com.achievement.domain.po.AchievementFieldDefs;
import com.achievement.mapper.AchievementFieldDefsMapper;
import com.achievement.service.IAchievementFieldDefsService;
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
public class AchievementFieldDefsServiceImpl extends ServiceImpl<AchievementFieldDefsMapper, AchievementFieldDefs> implements IAchievementFieldDefsService {

    private final StrapiClient strapiClient;

    @Override
    public JsonNode createFieldDef(Map<String, Object> req) {
        String raw = strapiClient.create("achievement-field-defs", req);
        try {
            return new ObjectMapper().readTree(raw);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JsonNode updateFieldDef(String fieldDefDocId, Map<String, Object> req) {
        
        String raw = strapiClient.update("achievement-field-defs", fieldDefDocId, req);
        try {
            return new ObjectMapper().readTree(raw);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
