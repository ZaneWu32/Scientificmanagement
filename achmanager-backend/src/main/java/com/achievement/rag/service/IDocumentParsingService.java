package com.achievement.rag.service;

import com.achievement.rag.model.ParseRequestVO;
import com.achievement.rag.model.ParseResultVO;
import com.achievement.rag.model.ParseTask;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档解析服务接口
 */
public interface IDocumentParsingService {

    /**
     * 上传文件并创建解析任务（异步处理）
     *
     * @param file      上传的文件
     * @param typeDocId 成果物类型的 documentId
     * @param userId    操作人用户 ID
     * @param userName  操作人用户名
     * @return 任务信息
     */
    ParseRequestVO uploadAndParse(MultipartFile file, String typeDocId,
                                  String userId, String userName);

    /**
     * 获取解析任务状态和结果
     *
     * @param taskId 任务 ID
     * @return 解析结果（含提取的字段）
     */
    ParseResultVO getParseResult(String taskId);

    /**
     * 直接获取内存中的 task（内部使用）
     */
    ParseTask getTask(String taskId);
}
