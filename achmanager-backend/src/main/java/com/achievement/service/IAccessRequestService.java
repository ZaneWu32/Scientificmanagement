package com.achievement.service;

import com.achievement.domain.dto.AccessRequestCreateDTO;
import com.achievement.domain.dto.AccessRequestQueryDTO;
import com.achievement.domain.dto.AccessRequestReviewDTO;
import com.achievement.domain.dto.KeycloakUser;
import com.achievement.domain.vo.AccessRequestVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 访问权限申请服务接口
 */
public interface IAccessRequestService {

    /**
     * 创建访问申请
     * @param achievementDocId 成果 documentId
     * @param createDTO 申请信息
     * @param requester 申请人
     * @return 申请结果
     */
    AccessRequestVO createAccessRequest(String achievementDocId, AccessRequestCreateDTO createDTO, KeycloakUser requester);

    /**
     * 分页查询访问申请列表
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    Page<AccessRequestVO> pageAccessRequests(AccessRequestQueryDTO queryDTO);
    
    /**
     * 审核访问申请
     * @param requestId 申请ID
     * @param reviewDTO 审核信息
     * @param reviewer 审核人
     * @return 审核结果
     */
    AccessRequestVO reviewAccessRequest(String requestId, AccessRequestReviewDTO reviewDTO, KeycloakUser reviewer);
}
