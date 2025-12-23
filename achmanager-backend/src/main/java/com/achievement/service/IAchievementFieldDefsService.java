package com.achievement.service;

import com.achievement.domain.po.AchievementFieldDefs;
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
public interface IAchievementFieldDefsService extends IService<AchievementFieldDefs> {

    JsonNode createFieldDef(Map<String, Object> req);

    JsonNode updateFieldDef(String fieldDefDocId, Map<String, Object> req);
}
