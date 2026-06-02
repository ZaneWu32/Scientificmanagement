package com.achievement.rag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 解析请求响应（upload 接口返回值）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParseRequestVO {
    private String taskId;
    private String fileId;
    private String fileName;
    private String parseStatus;
}
