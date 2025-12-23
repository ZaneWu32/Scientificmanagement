package com.achievement.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class AchTypeDetailVO {
    private Integer id;
    private String documentId;
    private String typeCode;
    private String typeName;
    private String description;
    private List<AchTypeDef> fieldDefinitions;

}
