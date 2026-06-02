package com.achievement.rag.model;

import com.achievement.domain.vo.AchTypeDef;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 解析结果 VO——返回给前端展示
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParseResultVO {
    private String taskId;
    private String parseStatus;
    private String errorMessage;

    /** 成果物类型信息 */
    private String typeCode;
    private String typeName;

    /** 该类型的完整字段定义（前端据此渲染表单） */
    private List<AchTypeDef> fieldDefinitions;

    /** 提取出的字段值集 */
    private List<ExtractedFieldValue> extractedFields;
}
