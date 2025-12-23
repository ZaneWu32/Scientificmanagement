package com.achievement.domain.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "成果物类型字段定义VO")
public class AchTypeDef {
    private Integer id;
    private String documentId;
    private String fieldCode;
    private  String fieldName;
    private String fieldType;
    private String description;
}
