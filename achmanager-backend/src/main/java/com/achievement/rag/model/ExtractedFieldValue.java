package com.achievement.rag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单个字段的提取结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedFieldValue {
    /** 字段编码（对应 AchTypeDef.fieldCode） */
    private String fieldCode;

    /** 字段名称（对应 AchTypeDef.fieldName） */
    private String fieldName;

    /** 字段类型（对应 AchTypeDef.fieldType） */
    private String fieldType;

    /** 提取出的值 */
    private String extractedValue;

    /** 置信度 0~1 */
    private double confidence;

    /** 提取方式：regex / llm */
    private String extractMethod;
}
