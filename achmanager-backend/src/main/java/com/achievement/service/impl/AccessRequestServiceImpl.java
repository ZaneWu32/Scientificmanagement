package com.achievement.service.impl;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.achievement.domain.dto.AccessRequestCreateDTO;
import com.achievement.domain.dto.AccessRequestQueryDTO;
import com.achievement.domain.dto.AccessRequestReviewDTO;
import com.achievement.domain.dto.KeycloakUser;
import com.achievement.domain.po.AchievementAccessRequest;
import com.achievement.domain.po.AchievementAccessGrant;
import com.achievement.domain.dto.AchMainBaseRow;
import com.achievement.domain.vo.AccessRequestVO;
import com.achievement.mapper.AchievementAccessGrantMapper;
import com.achievement.mapper.AchievementAccessRequestMapper;
import com.achievement.mapper.AchievementMainsMapper;
import com.achievement.service.IAccessRequestService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 访问权限申请服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccessRequestServiceImpl implements IAccessRequestService {

    private static final String REQUEST_STATUS_PENDING = "pending";
    private static final String REQUEST_STATUS_APPROVED = "approved";
    private static final String REQUEST_STATUS_REJECTED = "rejected";
    private static final String GRANT_STATUS_ACTIVE = "active";
    private static final String GRANT_SCOPE_FULL_CONTENT = "full_content";

    private final AchievementAccessGrantMapper accessGrantMapper;
    private final AchievementAccessRequestMapper accessRequestMapper;
    private final AchievementMainsMapper achievementMainsMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccessRequestVO createAccessRequest(
            String achievementDocId,
            AccessRequestCreateDTO createDTO,
            KeycloakUser requester) {

        if (requester == null || requester.getId() == null) {
            throw new RuntimeException("未登录");
        }
        if (requester.hasRole("research_admin")) {
            throw new RuntimeException("管理员无需申请访问");
        }
        log.info("提交访问申请: achievementDocId={}, requesterId={}", achievementDocId, requester.getId());

        AchMainBaseRow achievement = achievementMainsMapper.selectMainBaseByDocId(achievementDocId);
        if (achievement == null) {
            throw new RuntimeException("成果不存在");
        }
        if (!isApprovedAndPublished(achievement)) {
            throw new RuntimeException("成果尚未审核通过，暂不可申请访问");
        }
        if (!isSummaryVisible(achievement.getVisibilityRange())) {
            throw new RuntimeException("当前成果未开放摘要查看，无法发起访问申请");
        }
        if (isOwner(achievement, requester)) {
            throw new RuntimeException("成果创建人无需申请访问");
        }
        if (hasActiveGrant(achievementDocId, requester.getId())) {
            throw new RuntimeException("您已拥有该成果的完整访问权限");
        }

        AchievementAccessRequest latestRequest = findLatestRequest(achievementDocId, requester.getId());
        if (latestRequest != null) {
            if (REQUEST_STATUS_PENDING.equals(latestRequest.getStatus())) {
                throw new RuntimeException("您已提交申请，请等待管理员审核");
            }
            if (REQUEST_STATUS_APPROVED.equals(latestRequest.getStatus())) {
                throw new RuntimeException("您已拥有该成果的完整访问权限");
            }
        }

        String requesterName = requester.getName();
        if (requesterName == null || requesterName.isBlank()) {
            requesterName = requester.getUsername();
        }

        LocalDateTime now = LocalDateTime.now();
        AchievementAccessRequest entity = new AchievementAccessRequest()
                .setDocumentId(generateDocumentId())
                .setAchievementId(achievement.getMainId())
                .setAchievementDocId(achievementDocId)
                .setAchievementTitle(achievement.getTitle())
                .setRequesterId(requester.getId())
                .setRequesterName(requesterName == null ? "" : requesterName)
                .setReason(createDTO.getReason().trim())
                .setStatus(REQUEST_STATUS_PENDING)
                .setCreatedAt(now)
                .setUpdatedAt(now)
                .setCreatedById(null)
                .setUpdatedById(null)
                .setIsDelete(0);
        accessRequestMapper.insert(entity);

        AccessRequestVO result = new AccessRequestVO();
        result.setId(entity.getDocumentId());
        result.setResultId(achievementDocId);
        result.setResultTitle(achievement.getTitle());
        result.setResultType(achievement.getTypeName());
        result.setResultStatus(achievement.getAuditStatus());
        result.setProjectName(achievement.getProjectName());
        result.setVisibility(achievement.getVisibilityRange());
        result.setUserId(requester.getId());
        result.setUserName(entity.getRequesterName());
        result.setReason(entity.getReason());
        result.setStatus(entity.getStatus());
        result.setCreatedAt(entity.getCreatedAt());
        return result;
    }

    @Override
    public Page<AccessRequestVO> pageAccessRequests(AccessRequestQueryDTO queryDTO) {
        log.info("查询访问申请列表: keyword={}, status={}", queryDTO.getKeyword(), queryDTO.getStatus());

        // 参数校验
        int pageNum = (queryDTO.getPage() == null || queryDTO.getPage() < 1) ? 1 : queryDTO.getPage();
        int pageSize = (queryDTO.getPageSize() == null || queryDTO.getPageSize() < 1) ? 10 : queryDTO.getPageSize();
        if (pageSize > 100) {
            pageSize = 100;
        }

        // 创建分页对象
        Page<AccessRequestVO> page = new Page<>(pageNum, pageSize);

        // 执行查询
        Page<AccessRequestVO> result = accessRequestMapper.pageAccessRequests(page, queryDTO);

        log.info("查询访问申请完成: total={}, records={}", result.getTotal(), result.getRecords().size());

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccessRequestVO reviewAccessRequest(
            String requestId,
            AccessRequestReviewDTO reviewDTO,
            KeycloakUser reviewer) {

        log.info("审核访问申请: requestId={}, action={}, reviewerId={}",
                requestId, reviewDTO.getAction(), reviewer.getId());

        // 1. 查询申请记录
        LambdaQueryWrapper<AchievementAccessRequest> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AchievementAccessRequest::getDocumentId, requestId)
                .eq(AchievementAccessRequest::getIsDelete, 0);
        AchievementAccessRequest request = accessRequestMapper.selectOne(queryWrapper);

        if (request == null) {
            throw new RuntimeException("访问申请不存在");
        }

        // 2. 检查状态
        if (!REQUEST_STATUS_PENDING.equals(request.getStatus())) {
            throw new RuntimeException("该申请已被审核，无法重复审核");
        }

        String action = reviewDTO.getAction() == null ? "" : reviewDTO.getAction().trim().toLowerCase();
        if (!"approve".equals(action) && !"reject".equals(action)) {
            throw new RuntimeException("无效的审核动作: " + reviewDTO.getAction());
        }
        // 3. 确定新状态
        String newStatus = "approve".equals(action) ? REQUEST_STATUS_APPROVED : REQUEST_STATUS_REJECTED;

        String reviewerName = reviewer.getName();
        if (reviewerName == null || reviewerName.isBlank()) {
            reviewerName = reviewer.getUsername();
        }
        LocalDateTime now = LocalDateTime.now();

        // 4. 更新申请状态
        // Keycloak 业务用户不映射 Strapi admin_users，避免写入审计外键字段。
        LambdaUpdateWrapper<AchievementAccessRequest> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(AchievementAccessRequest::getDocumentId, requestId)
                .set(AchievementAccessRequest::getStatus, newStatus)
                .set(AchievementAccessRequest::getReviewerId, reviewer.getId())
                .set(AchievementAccessRequest::getReviewerName, reviewerName)
                .set(AchievementAccessRequest::getReviewComment, reviewDTO.getComment())
                .set(AchievementAccessRequest::getReviewedAt, now)
                .set(AchievementAccessRequest::getUpdatedAt, now)
                .set(AchievementAccessRequest::getUpdatedById, null);

        accessRequestMapper.update(null, updateWrapper);

        if (REQUEST_STATUS_APPROVED.equals(newStatus)) {
            upsertActiveGrant(request, reviewer, reviewerName, now);
        }

        // 5. 构建返回结果
        AccessRequestVO result = new AccessRequestVO();
        result.setId(requestId);
        result.setResultId(request.getAchievementDocId());
        result.setResultTitle(request.getAchievementTitle());
        result.setUserId(request.getRequesterId());
        result.setUserName(request.getRequesterName());
        result.setReason(request.getReason());
        result.setStatus(newStatus);
        result.setReviewer(reviewerName);
        result.setComment(reviewDTO.getComment());
        result.setCreatedAt(request.getCreatedAt());
        result.setReviewedAt(now);

        log.info("访问申请审核完成: requestId={}, newStatus={}", requestId, newStatus);

        return result;
    }

    private AchievementAccessRequest findLatestRequest(String achievementDocId, Integer requesterId) {
        if (achievementDocId == null || achievementDocId.isBlank() || requesterId == null) {
            return null;
        }
        LambdaQueryWrapper<AchievementAccessRequest> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AchievementAccessRequest::getAchievementDocId, achievementDocId)
                .eq(AchievementAccessRequest::getRequesterId, requesterId)
                .eq(AchievementAccessRequest::getIsDelete, 0)
                .orderByDesc(AchievementAccessRequest::getCreatedAt)
                .orderByDesc(AchievementAccessRequest::getId)
                .last("LIMIT 1");
        return accessRequestMapper.selectOne(queryWrapper);
    }

    private boolean isOwner(AchMainBaseRow achievement, KeycloakUser requester) {
        return achievement != null
                && requester != null
                && requester.getId() != null
                && achievement.getCreatedByUserId() != null
                && achievement.getCreatedByUserId().equals(String.valueOf(requester.getId()));
    }

    private boolean isApprovedAndPublished(AchMainBaseRow achievement) {
        return achievement != null
                && "APPROVED".equalsIgnoreCase(achievement.getAuditStatus())
                && achievement.getPublishedAt() != null;
    }

    private boolean isSummaryVisible(String visibilityRange) {
        if (visibilityRange == null) {
            return false;
        }
        return switch (visibilityRange.trim().toLowerCase()) {
            case "public", "internal", "public_abstract", "internal_abstract" -> true;
            default -> false;
        };
    }

    private boolean hasActiveGrant(String achievementDocId, Integer granteeId) {
        if (achievementDocId == null || achievementDocId.isBlank() || granteeId == null) {
            return false;
        }
        LambdaQueryWrapper<AchievementAccessGrant> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AchievementAccessGrant::getAchievementDocId, achievementDocId)
                .eq(AchievementAccessGrant::getGranteeId, granteeId)
                .eq(AchievementAccessGrant::getStatus, GRANT_STATUS_ACTIVE)
                .eq(AchievementAccessGrant::getIsDelete, 0)
                .isNull(AchievementAccessGrant::getRevokedAt)
                .and(wrapper -> wrapper.isNull(AchievementAccessGrant::getExpiresAt)
                        .or()
                        .gt(AchievementAccessGrant::getExpiresAt, LocalDateTime.now()))
                .last("LIMIT 1");
        return accessGrantMapper.selectOne(queryWrapper) != null;
    }

    private void upsertActiveGrant(
            AchievementAccessRequest request,
            KeycloakUser reviewer,
            String reviewerName,
            LocalDateTime now) {

        LambdaQueryWrapper<AchievementAccessGrant> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AchievementAccessGrant::getAchievementDocId, request.getAchievementDocId())
                .eq(AchievementAccessGrant::getGranteeId, request.getRequesterId())
                .eq(AchievementAccessGrant::getIsDelete, 0)
                .orderByDesc(AchievementAccessGrant::getGrantedAt)
                .orderByDesc(AchievementAccessGrant::getId)
                .last("LIMIT 1");
        AchievementAccessGrant existingGrant = accessGrantMapper.selectOne(queryWrapper);

        if (existingGrant != null) {
            LambdaUpdateWrapper<AchievementAccessGrant> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(AchievementAccessGrant::getId, existingGrant.getId())
                    .set(AchievementAccessGrant::getStatus, GRANT_STATUS_ACTIVE)
                    .set(AchievementAccessGrant::getGrantScope, GRANT_SCOPE_FULL_CONTENT)
                    .set(AchievementAccessGrant::getSourceRequestId, request.getId())
                    .set(AchievementAccessGrant::getSourceRequestDocId, request.getDocumentId())
                    .set(AchievementAccessGrant::getGrantedById, reviewer.getId())
                    .set(AchievementAccessGrant::getGrantedByName, reviewerName == null ? "" : reviewerName)
                    .set(AchievementAccessGrant::getGrantedAt, now)
                    .set(AchievementAccessGrant::getRevokedAt, null)
                    .set(AchievementAccessGrant::getRevokeReason, null)
                    .set(AchievementAccessGrant::getUpdatedAt, now)
                    .set(AchievementAccessGrant::getUpdatedById, null);
            accessGrantMapper.update(null, updateWrapper);
            return;
        }

        AchievementAccessGrant grant = new AchievementAccessGrant()
                .setDocumentId(generateGrantDocumentId())
                .setAchievementId(request.getAchievementId())
                .setAchievementDocId(request.getAchievementDocId())
                .setGranteeId(request.getRequesterId())
                .setGranteeName(request.getRequesterName())
                .setSourceRequestId(request.getId())
                .setSourceRequestDocId(request.getDocumentId())
                .setGrantedById(reviewer.getId())
                .setGrantedByName(reviewerName == null ? "" : reviewerName)
                .setGrantScope(GRANT_SCOPE_FULL_CONTENT)
                .setStatus(GRANT_STATUS_ACTIVE)
                .setGrantedAt(now)
                .setCreatedAt(now)
                .setUpdatedAt(now)
                .setCreatedById(null)
                .setUpdatedById(null)
                .setIsDelete(0);
        accessGrantMapper.insert(grant);
    }

    private String generateDocumentId() {
        return "access_req_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String generateGrantDocumentId() {
        return "access_grant_" + UUID.randomUUID().toString().replace("-", "");
    }
}
