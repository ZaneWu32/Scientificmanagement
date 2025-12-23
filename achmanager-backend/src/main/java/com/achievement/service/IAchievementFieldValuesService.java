package com.achievement.service;

import com.achievement.domain.po.AchievementFieldValues;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author author
 * @since 2025-12-11
 */
public interface IAchievementFieldValuesService extends IService<AchievementFieldValues> {

    JsonNode createFieldValue(Map<String, Object> req);

    JsonNode updateFieldValue(String fieldValueDocId, Map<String, Object> req);
}
