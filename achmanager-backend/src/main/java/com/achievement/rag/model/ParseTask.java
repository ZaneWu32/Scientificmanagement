package com.achievement.rag.model;

import com.achievement.domain.vo.AchTypeDef;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 文档解析任务（内存态，用于异步状态跟踪）
 */
@Data
public class ParseTask {
    private String taskId;
    private String fileId;
    private String fileName;
    private String originalName;
    private long fileSize;
    private String mimeType;
    private String filePath;

    /** pending → parsing → cleaning → extracting → completed / failed */
    private String parseStatus;
    private String errorMessage;

    /** 成果物类型信息 */
    private String typeDocId;
    private String typeCode;
    private String typeName;

    /** 该类型的字段定义（从 DB 动态查询） */
    private List<AchTypeDef> fieldDefinitions;

    /** 清洗后文本 */
    private String cleanedText;

    /** 提取结果 */
    private List<ExtractedFieldValue> extractedFields = new CopyOnWriteArrayList<>();

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public ParseTask() {
        this.createTime = LocalDateTime.now();
        this.updateTime = this.createTime;
        this.parseStatus = "pending";
    }
}
