package com.achievement.service.impl;

import com.achievement.client.StrapiClient;
import com.achievement.domain.dto.*;
import com.achievement.domain.po.AchievementAccessGrant;
import com.achievement.domain.po.AchievementAccessRequest;
import com.achievement.domain.po.AchievementMains;
import com.achievement.domain.vo.*;
import com.achievement.mapper.AchievementAccessGrantMapper;
import com.achievement.mapper.AchievementAccessRequestMapper;
import com.achievement.mapper.AchievementMainsMapper;
import com.achievement.service.IAchievementMainsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.achievement.constant.AchievementStatusConstant.APPROVED;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author author
 * @since 2025-12-11
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AchievementMainsServiceImpl extends ServiceImpl<AchievementMainsMapper, AchievementMains> implements IAchievementMainsService {
    private static final String ACHIEVEMENT_FILE_COLLECTION = "achievement-files";
    private static final DateTimeFormatter REQUEST_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final StrapiClient strapiClient;
    private final ObjectMapper objectMapper;
    private final AchievementMainsMapper mainsMapper;
    private final AchievementAccessGrantMapper accessGrantMapper;
    private final AchievementAccessRequestMapper accessRequestMapper;
    private final KeycloakUserServiceImpl keycloakUserServiceImpl;
    @Override
    public Page<AchListVO> pageList(AchListDTO achListDTO) {
        int pageNum  = (achListDTO.getPageNum()  == null || achListDTO.getPageNum()  < 1)  ? 1  : achListDTO.getPageNum();
        int pageSize = (achListDTO.getPageSize() == null || achListDTO.getPageSize() < 1) ? 10 : achListDTO.getPageSize();
        if (pageSize > 100) pageSize = 100;

        Page<AchListVO> page = new Page<>(pageNum, pageSize);
        page.setOptimizeCountSql(false);

        // 1) 执行分页查询
        Page<AchListVO> result = baseMapper.pageList(page, achListDTO);

        List<AchListVO> records = result.getRecords();
        if (records == null || records.isEmpty()) {
            return result;
        }

        // 2) 本页缓存：creatorId -> creatorName
        Map<String, String> cache = new HashMap<>();

        for (AchListVO vo : records) {
            String creatorIdStr = vo.getCreatorId();
            if (creatorIdStr == null || creatorIdStr.isBlank()) {
                vo.setCreatorName("-");
                continue;
            }

            // 命中缓存
            if (cache.containsKey(creatorIdStr)) {
                vo.setCreatorName(cache.get(creatorIdStr));
                continue;
            }

            String creatorName = null;
            try {
                int creatorId = Integer.parseInt(creatorIdStr);
                KeycloakUser user = keycloakUserServiceImpl.getUserById(creatorId);
                if (user != null && user.getUsername() != null && !user.getUsername().isBlank()) {
                    creatorName = user.getUsername();
                }
            } catch (NumberFormatException e) {
                // creatorId 不是数字（比如 UUID），这里就不能 parseInt
            } catch (Exception e) {
                // keycloak 查询失败
            }

            if (creatorName == null) creatorName = "-";

            cache.put(creatorIdStr, creatorName);
            vo.setCreatorName(creatorName);
        }

        return result;
    }


    @Override
    public Page<AchListVO> pageList4User(AchListDTO achListDTO, Integer userId){// 兜底：防止 null / 非法值（即使没有校验或校验被改了，其实也安全）
        int pageNum  = (achListDTO.getPageNum()  == null || achListDTO.getPageNum()  < 1)  ? 1  : achListDTO.getPageNum();
        int pageSize = (achListDTO.getPageSize() == null || achListDTO.getPageSize() < 1) ? 10 : achListDTO.getPageSize();
        if (pageSize > 100) {
            pageSize = 100; // 防止一次性查太多
        }
        achListDTO.setCreatorId(userId);
        //MybatisPlus的分页查询
        Page<AchListVO> page = new Page<>(pageNum, pageSize);
        page.setOptimizeCountSql(false);  // 关键：关闭 count SQL 优化解析
        return baseMapper.pageList(page, achListDTO);
    }

    @Override
    public AchDetailVO selectDetailForProjectSystem(String achDocId) {
        return selectDetail(achDocId, null, true);
    }

    @Override
    public AchDetailVO selectDetail(String achDocId, KeycloakUser currentUser, boolean includeRestrictedContent) {
        AchMainBaseRow base = baseMapper.selectMainBaseByDocId(achDocId);
        if (base == null) {
            throw new RuntimeException("成果物不存在或已删除");
        }
        log.info("查询成果物详情，achDocId={}, includeRestrictedContent={}", achDocId, includeRestrictedContent);

        boolean owner = isOwner(base, currentUser);
        boolean admin = includeRestrictedContent || (currentUser != null && currentUser.hasRole("research_admin"));
        boolean reviewerAccess = canReviewFullContent(base, currentUser);
        boolean grantAccess = currentUser != null && currentUser.getId() != null && hasActiveGrant(achDocId, currentUser.getId());
        boolean approvedAndPublished = isApprovedAndPublished(base);
        boolean summaryVisible = isSummaryVisible(base.getVisibilityRange());

        if (!admin && !owner && !reviewerAccess && !grantAccess) {
            if (!approvedAndPublished) {
                throw new RuntimeException("成果尚未通过审核，无法查看");
            }
            if (!summaryVisible) {
                throw new RuntimeException("无权限查看该成果");
            }
        }

        AchievementAccessRequest latestRequest = null;
        if (currentUser != null && currentUser.getId() != null && !admin && !owner) {
            latestRequest = findLatestAccessRequest(achDocId, currentUser.getId());
        }
        boolean fullAccess = admin || owner || reviewerAccess || grantAccess;
        boolean effectiveSummaryVisible = fullAccess || summaryVisible;

        List<AchFieldRow> rows = Collections.emptyList();
        if (fullAccess && base.getMainId() != null && base.getTypeId() != null) {
            rows = baseMapper.selectFieldRows(base.getMainId(), base.getTypeId());
        }

        List<AchFieldVO> fields = rows.stream().map(r -> {
            AchFieldVO vo = new AchFieldVO();
            vo.setId(r.getFieldId());
            vo.setDocumentId(r.getDocumentId());
            vo.setFieldValueDocumentId(r.getFieldValueDocumentId());
            vo.setFieldCode(r.getFieldCode());
            vo.setFieldName(r.getFieldName());
            vo.setFieldType(r.getFieldType());
            vo.setIsRequired(r.getIsRequired());

            vo.setValue(pickValueByType(r));
            return vo;
        }).toList();

        AchDetailVO vo = new AchDetailVO();
        String creatorName = "-";
        if (base.getCreatedByUserId() != null && !base.getCreatedByUserId().isBlank()) {
            try {
                int creatorId = Integer.parseInt(base.getCreatedByUserId());
                KeycloakUser user = keycloakUserServiceImpl.getUserById(creatorId);
                if (user != null) {
                    if (user.getUsername() != null && !user.getUsername().isBlank()) {
                        creatorName = user.getUsername();
                    } else if (user.getName() != null && !user.getName().isBlank()) {
                        creatorName = user.getName();
                    }
                }
            } catch (NumberFormatException e) {
                log.warn("成果详情：createdByUserId 非数字，createdByUserId={}", base.getCreatedByUserId());
            } catch (Exception e) {
                log.warn("成果详情：查询创建者失败，createdByUserId={}", base.getCreatedByUserId(), e);
            }
        }
        vo.setCreatorName(creatorName);
        vo.setDocumentId(base.getDocumentId());

        vo.setTitle(base.getTitle());
        vo.setSummary(base.getSummary());
        vo.setAuditStatus(base.getAuditStatus());
        vo.setReviewerName(base.getReviewerName());
        vo.setReviewComment(base.getReviewComment());
        vo.setReviewedAt(base.getReviewedAt());
        vo.setCreatorName(creatorName);
        vo.setCreatedAt(base.getCreatedAt());
        vo.setUpdatedAt(base.getUpdatedAt());
        vo.setPublishedAt(base.getPublishedAt());

        vo.setTypeDocId(base.getTypeDocId());
        vo.setTypeName(base.getTypeName());
        vo.setTypeCode(base.getTypeCode());

        // 设置基础字段
        vo.setYear(base.getYear());
        vo.setProjectCode(base.getProjectCode());
        vo.setProjectName(base.getProjectName());
        vo.setVisibilityRange(base.getVisibilityRange());
        vo.setPermissionStatus(fullAccess ? "full" : (effectiveSummaryVisible ? "summary" : "denied"));
        vo.setAccessRequestStatus(latestRequest == null ? "none" : latestRequest.getStatus());
        vo.setCanRequestAccess(currentUser != null && !fullAccess && approvedAndPublished
                && effectiveSummaryVisible && canRequestAccess(latestRequest));
        if (latestRequest != null) {
            vo.setLastRequestAt(formatRequestTime(latestRequest.getCreatedAt()));
            if ("rejected".equalsIgnoreCase(latestRequest.getStatus())) {
                vo.setRejectedReason(latestRequest.getReviewComment());
            }
        }
        vo.setSummaryVisible(effectiveSummaryVisible);
        vo.setFullContentVisible(fullAccess);
        vo.setAttachmentDownloadAllowed(fullAccess);

        // 解析 JSON 数组字段
        vo.setAuthors(parseJsonArray(base.getAuthorsJson()));
        vo.setKeywords(parseJsonArray(base.getKeywordsJson()));

        vo.setFields(fields);

        if (fullAccess) {
            // 附件信息：通过 Strapi 查询 achievement_files，并 populate media(files)
            try {
                Map<String, String> params = new LinkedHashMap<>();
                params.put("filters[achievement_main_id][documentId][$eq]", achDocId);
                params.put("filters[is_delete][$eq]", "0");
                params.put("populate", "files");
                String raw = strapiClient.query(ACHIEVEMENT_FILE_COLLECTION, params);
                vo.setAttachments(objectMapper.readTree(raw));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        } else {
            vo.setAttachments(emptyAttachments());
        }

        return vo;
    }

    private List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    private AchievementAccessRequest findLatestAccessRequest(String achievementDocId, Integer requesterId) {
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

    private boolean hasActiveGrant(String achievementDocId, Integer granteeId) {
        if (achievementDocId == null || achievementDocId.isBlank() || granteeId == null) {
            return false;
        }
        LambdaQueryWrapper<AchievementAccessGrant> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AchievementAccessGrant::getAchievementDocId, achievementDocId)
                .eq(AchievementAccessGrant::getGranteeId, granteeId)
                .eq(AchievementAccessGrant::getStatus, "active")
                .eq(AchievementAccessGrant::getIsDelete, 0)
                .isNull(AchievementAccessGrant::getRevokedAt)
                .and(wrapper -> wrapper.isNull(AchievementAccessGrant::getExpiresAt)
                        .or()
                        .gt(AchievementAccessGrant::getExpiresAt, LocalDateTime.now()))
                .last("LIMIT 1");
        return accessGrantMapper.selectOne(queryWrapper) != null;
    }

    private boolean isOwner(AchMainBaseRow base, KeycloakUser currentUser) {
        return base != null
                && currentUser != null
                && currentUser.getId() != null
                && base.getCreatedByUserId() != null
                && base.getCreatedByUserId().equals(String.valueOf(currentUser.getId()));
    }

    private boolean isApprovedAndPublished(AchMainBaseRow base) {
        return base != null
                && "APPROVED".equalsIgnoreCase(base.getAuditStatus())
                && base.getPublishedAt() != null;
    }

    private boolean isSummaryVisible(String visibilityRange) {
        if (visibilityRange == null || visibilityRange.isBlank()) {
            return false;
        }
        return switch (visibilityRange.trim().toLowerCase(Locale.ROOT)) {
            case "public", "internal", "public_abstract", "internal_abstract", "public_full", "internal_full" -> true;
            default -> false;
        };
    }

    private boolean canRequestAccess(AchievementAccessRequest latestRequest) {
        if (latestRequest != null && "pending".equalsIgnoreCase(latestRequest.getStatus())) {
            return false;
        }
        if (latestRequest != null && "approved".equalsIgnoreCase(latestRequest.getStatus())) {
            return false;
        }
        return true;
    }

    private boolean canReviewFullContent(AchMainBaseRow base, KeycloakUser currentUser) {
        if (base == null || currentUser == null || currentUser.getId() == null) {
            return false;
        }
        if (currentUser.hasRole("research_admin")) {
            return true;
        }
        if (!currentUser.hasRole("research_expert")) {
            return false;
        }
        String status = base.getAuditStatus() == null ? "" : base.getAuditStatus().trim().toUpperCase(Locale.ROOT);
        if (!"UNDER_REVIEW".equals(status) && !"REVIEWING".equals(status)) {
            return false;
        }
        return base.getReviewerId() == null || currentUser.getId().equals(base.getReviewerId());
    }

    private String formatRequestTime(LocalDateTime time) {
        return time == null ? null : REQUEST_TIME_FORMATTER.format(time);
    }

    private JsonNode emptyAttachments() {
        return objectMapper.createObjectNode().putArray("data");
    }

    @Override
    public Page<AchListVO> pageList4Visibility(AchListDTO2 achListDTO) {
        int pageNum  = (achListDTO.getPageNum()  == null || achListDTO.getPageNum()  < 1)  ? 1  : achListDTO.getPageNum();
        int pageSize = (achListDTO.getPageSize() == null || achListDTO.getPageSize() < 1) ? 10 : achListDTO.getPageSize();
        if (pageSize > 100) {
            pageSize = 100; // 防止一次性查太多
        }
        //MybatisPlus的分页查询
        Page<AchListVO> page = new Page<>(pageNum, pageSize);
        page.setOptimizeCountSql(false);  // 关键：关闭 count SQL 优化解析
        return baseMapper.pageList2(page, achListDTO);
    }

    @Override
    public UserStatVo countstatistics() {
        // 本月时间范围 [本月第一天00:00:00, 下月第一天00:00:00)
        LocalDate now = LocalDate.now();
        LocalDateTime start = now.withDayOfMonth(1).atStartOfDay();
        LocalDateTime end = now.plusMonths(1).withDayOfMonth(1).atStartOfDay();

        Integer total = mainsMapper.countTotal();
        Integer monthNew = mainsMapper.countMonthNew( start, end);
        Integer paperCount = mainsMapper.countByTypeCode( "paper");
        Integer patentCount = mainsMapper.countByTypeCode( "patent");

        UserStatVo vo = new UserStatVo();
        vo.setTotalResults(total == null ? 0 : total);
        vo.setMonthlyNew(monthNew == null ? 0 : monthNew);
        vo.setPaperCount(paperCount == null ? 0 : paperCount);
        vo.setPatentCount(patentCount == null ? 0 : patentCount);
        return vo;
    }

    @Override
    public TypeYearTrendVo typeYearTrend(TrendQueryDTO dto) {
        // 1) 计算年份范围：优先用 fromYear/toYear，其次用 range(3y/5y)
        LocalDate now = LocalDate.now();
        int toYear = (dto.getToYear() != null) ? dto.getToYear() : now.getYear();

        int fromYear;
        if (dto.getFromYear() != null) {
            fromYear = dto.getFromYear();
        } else {
            int span = "3y".equalsIgnoreCase(dto.getRange()) ? 3 : 5;
            fromYear = toYear - (span - 1);
        }

        int years = toYear - fromYear + 1;
        if (years <= 0) years = 1;

        // 2) 查库：year + type + total + approved
        List<TypeYearTrendRow> rows = mainsMapper.countTypeYearTrend(fromYear, toYear);

        // 3) timeline 补齐
        List<String> timeline = new ArrayList<>();
        for (int y = fromYear; y <= toYear; y++) {
            timeline.add(String.valueOf(y));
        }

        // 4) 聚合：typeCode -> series（并补齐每年）
        Map<String, TypeYearTrendSeries> typeMap = new LinkedHashMap<>();

        if (rows != null) {
            for (TypeYearTrendRow r : rows) {
                if (r == null || r.getYear() == null) continue;

                String typeCode = r.getTypeCode() == null ? "" : r.getTypeCode().trim();
                if (typeCode.isEmpty()) typeCode = "UNKNOWN";

                String typeName = r.getTypeName() == null ? "" : r.getTypeName().trim();
                if (typeName.isEmpty()) typeName = typeCode;

                // ✅ lambda 里只用 k（final），不要引用外部会变化的 typeName
                int finalYears = years;
                TypeYearTrendSeries s = typeMap.computeIfAbsent(typeCode, k -> {
                    TypeYearTrendSeries ns = new TypeYearTrendSeries();
                    ns.setTypeCode(k);
                    ns.setTotal(initZeros(finalYears));
                    ns.setApproved(initZeros(finalYears));
                    return ns;
                });

                // ✅ typeName 放到 lambda 外设置（避免“有效 final”问题）
                if (s.getTypeName() == null || s.getTypeName().isEmpty()) {
                    s.setTypeName(typeName);
                }

                int idx = r.getYear() - fromYear;
                if (idx >= 0 && idx < years) {
                    s.getTotal().set(idx, safeInt(r.getTotalCount()));
                    s.getApproved().set(idx, safeInt(r.getApprovedCount()));
                }
            }
        }

        // 5) 总计两条线：所有类型加总
        List<Integer> totalCounts = initZeros(years);
        List<Integer> approvedCounts = initZeros(years);
        for (TypeYearTrendSeries s : typeMap.values()) {
            for (int i = 0; i < years; i++) {
                totalCounts.set(i, totalCounts.get(i) + safeInt(s.getTotal().get(i)));
                approvedCounts.set(i, approvedCounts.get(i) + safeInt(s.getApproved().get(i)));
            }
        }

        // 6) 输出
        TypeYearTrendVo vo = new TypeYearTrendVo();
        vo.setTimeline(timeline);
        vo.setSeries(new ArrayList<>(typeMap.values()));
        vo.setTotalCounts(totalCounts);
        vo.setApprovedCounts(approvedCounts);
        return vo;
    }



    private List<Integer> initZeros(int n) {
        List<Integer> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) list.add(0);
        return list;
    }

    private int safeInt(Integer v) {
        return v == null ? 0 : v;
    }

    // private int safeInt(Object v) {
    //     return (v instanceof Integer) ? (Integer) v : 0;
    // }

    private Object pickValueByType(AchFieldRow r) {
        if (r.getFieldType() == null) return r.getTextValue();
        String normalizedType = r.getFieldType().trim().toUpperCase(Locale.ROOT);
        return switch (normalizedType) {
            case "TEXT", "RICHTEXT", "JSON", "MEDIA" -> r.getTextValue();
            case "BOOLEAN" -> r.getBooleanValue();
            case "NUMBER" -> r.getNumberValue();
            case "DATE", "DATETIME" -> r.getDateValue();
            case "EMAIL" -> r.getEmailValue();
            default -> r.getTextValue();
        };
    }

    @Override
    public UserStatVo countByUserId(Integer userId) {
        if (userId == null) {
            throw new RuntimeException("userId不能为空");
        }

        // 本月时间范围 [本月第一天00:00:00, 下月第一天00:00:00)
        LocalDate now = LocalDate.now();
        LocalDateTime start = now.withDayOfMonth(1).atStartOfDay();
        LocalDateTime end = now.plusMonths(1).withDayOfMonth(1).atStartOfDay();

        Integer total = mainsMapper.countTotalByUser(userId);
        Integer monthNew = mainsMapper.countMonthNewByUser(userId, start, end);
        Integer paperCount = mainsMapper.countByTypeCodeForUser(userId, "paper");
        Integer patentCount = mainsMapper.countByTypeCodeForUser(userId, "patent");
        // ===== 近年趋势：默认近 5 年（含今年）=====
        int years = 5;
        int fromYear = now.getYear() - (years - 1);
        int toYear = now.getYear();

        // 一次性查出这几年每年的数量（可能缺年）
        List<YearTrendItem> dbTrend = mainsMapper.countYearlyTrendByUser(userId, fromYear, toYear);

        // 补齐缺失年份（保证前端 xAxis 连续）
        Map<Integer, Integer> year2count = new HashMap<>();
        if (dbTrend != null) {
            for (YearTrendItem it : dbTrend) {
                year2count.put(it.getYear(), it.getCount() == null ? 0 : it.getCount());
            }
        }
        List<YearTrendItem> fullTrend = new ArrayList<>();
        for (int y = fromYear; y <= toYear; y++) {
            YearTrendItem item = new YearTrendItem();
            item.setYear(y);
            item.setCount(year2count.getOrDefault(y, 0));
            fullTrend.add(item);
        }

        UserStatVo vo = new UserStatVo();
        vo.setTotalResults(total == null ? 0 : total);
        vo.setMonthlyNew(monthNew == null ? 0 : monthNew);
        vo.setPaperCount(paperCount == null ? 0 : paperCount);
        vo.setPatentCount(patentCount == null ? 0 : patentCount);
        vo.setYearlyTrend(fullTrend);
        return vo;
    }

    @Override
    public Long countByUserIdAndTypeId(Long typeId, Long userId) {
        return 0L;
    }

    @Override
    public Long countMonthNewByUserId(Long userId) {
        return 0L;
    }



    /*@Override
    public Long countByUserIdAndTypeId(Long typeId, Long userId) {
        if (userId == null || typeId == null) {
            return 0L;
        }
        return this.lambdaQuery()
                //TODO 数据表尚未修改 .eq(AchievementMains::getCreatorId, userId)
                .eq(AchievementMains::getIsDelete, 0)
                .isNotNull(AchievementMains::getPublishedAt) // ✅ 只要已发布
                .inSql(AchievementMains::getId,
                        "SELECT achievement_main_id " +
                                "FROM achievement_mains_achievement_type_id_lnk " +
                                "WHERE achievement_type_id = " + typeId
                )
                .count();
    }
    @Override
    public Long countMonthNewByUserId(Long userId) {
        if (userId == null) {
            return 0L;
        }
        LocalDate now = LocalDate.now();
        LocalDateTime firstDay = now.withDayOfMonth(1).atStartOfDay(); // 本月1号 00:00:00
        LocalDateTime firstDayNextMonth = firstDay.plusMonths(1); // 下月1号 00:00:00
        Long count = this.lambdaQuery()
                //TODO 数据表尚未修改 .eq(AchievementMains::getCreatorId, userId)
                .eq(AchievementMains::getIsDelete, 0)
                .eq(AchievementMains::getAchievementStatus,APPROVED) //具体可修改，此处规定为审核通过的成果物
                .isNotNull(AchievementMains::getPublishedAt) // ✅ 只要已发布
                .ge(AchievementMains::getCreatedAt, firstDay)        // >= 本月1号 00:00:00
                .lt(AchievementMains::getCreatedAt, firstDayNextMonth) // < 下月1号 00:00:00

                .count();
        return count;
    }*/
    @Override
    public Page<AchListVO> pageList4Admin(AchListDTO2 achListDTO) {
        int pageNum  = (achListDTO.getPageNum()  == null || achListDTO.getPageNum()  < 1)  ? 1  : achListDTO.getPageNum();
        int pageSize = (achListDTO.getPageSize() == null || achListDTO.getPageSize() < 1) ? 10 : achListDTO.getPageSize();
        if (pageSize > 100) {
            pageSize = 100; // 防止一次性查太多
        }
        //MybatisPlus的分页查询
        Page<AchListVO> page = new Page<>(pageNum, pageSize);
        page.setOptimizeCountSql(false);  // 关键：关闭 count SQL 优化解析
        return baseMapper.pageList4admin(page, achListDTO);
    }

    @Override
    public Page<AchListVO> pageList4UserAch(AchListDTO achListDTO, Integer userId) {
        int pageNum  = (achListDTO.getPageNum()  == null || achListDTO.getPageNum()  < 1)  ? 1  : achListDTO.getPageNum();
        int pageSize = (achListDTO.getPageSize() == null || achListDTO.getPageSize() < 1) ? 10 : achListDTO.getPageSize();
        if (pageSize > 100) {
            pageSize = 100; // 防止一次性查太多
        }
        achListDTO.setCreatorId(userId);
        log.info("用户id:{}",userId);
        //MybatisPlus的分页查询
        Page<AchListVO> page = new Page<>(pageNum, pageSize);
        page.setOptimizeCountSql(false);  // 关键：关闭 count SQL 优化解析
        return baseMapper.pageListMySelf(page, achListDTO);
    }

    @Override
    public Long countByTypeId(Long typeId) {

        return this.lambdaQuery()
                .eq(AchievementMains::getIsDelete, 0)
                .isNotNull(AchievementMains::getPublishedAt) // ✅ 只要已发布
                .inSql(AchievementMains::getId,
                        "SELECT achievement_main_id " +
                                "FROM achievement_mains_achievement_type_id_lnk " +
                                "WHERE achievement_type_id = " + typeId
                )
                .count();
    }

    @Override
    public Long countAch() {
        return this.lambdaQuery()
                .isNotNull(AchievementMains::getPublishedAt) // ✅ 只要已发布;
                .eq(AchievementMains::getIsDelete, 0)
                .count();
    }

    @Override
    public Long countMonthNew() {
        LocalDate now = LocalDate.now();
        LocalDateTime firstDay = now.withDayOfMonth(1).atStartOfDay(); // 本月1号 00:00:00
        LocalDateTime firstDayNextMonth = firstDay.plusMonths(1); // 下月1号 00:00:00
        Long count = this.lambdaQuery()
                .eq(AchievementMains::getIsDelete, 0)
                .eq(AchievementMains::getAchievementStatus,APPROVED) //具体可修改，此处规定为审核通过的成果物
                .isNotNull(AchievementMains::getPublishedAt) // ✅ 只要已发布
                .ge(AchievementMains::getCreatedAt, firstDay)        // >= 本月1号 00:00:00
                .lt(AchievementMains::getCreatedAt, firstDayNextMonth) // < 下月1号 00:00:00
                .count();
        return count;
    }
}
