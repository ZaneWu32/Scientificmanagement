package com.achievement.service.impl;

import com.achievement.domain.dto.AchListDTO;
import com.achievement.domain.po.AchievementMains;
import com.achievement.domain.vo.AchListVO;
import com.achievement.mapper.AchievementMainsMapper;
import com.achievement.service.IAchievementMainsService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
public class AchievementMainsServiceImpl extends ServiceImpl<AchievementMainsMapper, AchievementMains> implements IAchievementMainsService {
    @Override
    public Page<AchListVO> pageList(AchListDTO achListDTO){// 兜底：防止 null / 非法值（即使没有校验或校验被改了，其实也安全）
        int pageNum  = (achListDTO.getPageNum()  == null || achListDTO.getPageNum()  < 1)  ? 1  : achListDTO.getPageNum();
        int pageSize = (achListDTO.getPageSize() == null || achListDTO.getPageSize() < 1) ? 10 : achListDTO.getPageSize();
        if (pageSize > 100) {
            pageSize = 100; // 防止一次性查太多
        }
        //MybatisPlus的分页查询
        Page<AchListVO> page = new Page<>(pageNum, pageSize);
        return baseMapper.pageList(page, achListDTO);
    }
    @Override
    public Page<AchListVO> pageList4User(AchListDTO achListDTO){// 兜底：防止 null / 非法值（即使没有校验或校验被改了，其实也安全）
        int pageNum  = (achListDTO.getPageNum()  == null || achListDTO.getPageNum()  < 1)  ? 1  : achListDTO.getPageNum();
        int pageSize = (achListDTO.getPageSize() == null || achListDTO.getPageSize() < 1) ? 10 : achListDTO.getPageSize();
        if (pageSize > 100) {
            pageSize = 100; // 防止一次性查太多
        }
        Long userId = 1L; //TODO 获取当前用户ID
        //TODO achListDTO.setCreatorId(userId);
        //MybatisPlus的分页查询
        Page<AchListVO> page = new Page<>(pageNum, pageSize);
        return baseMapper.pageList(page, achListDTO);
    }


    @Override
    public Long countByUserId() {
        Long userId = 1L; //TODO 获取当前用户ID ,
        Long count = 0L;
        /*Long count = this.lambdaQuery()
                .eq(AchievementMains::getCreatorId, userId)
                .isNotNull(AchievementTypes::getPublishedAt) // ✅ 只要已发布
                .count();*/ //TODO 数据表尚未修改
        return count;
    }

    @Override
    public Long countByUserIdAndTypeId(Long typeId) {
        Long userId = 1L; //TODO 获取当前用户ID
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
    public Long countMonthNewByUserId() {
        Long userId = 1L;//TODO 获取当前用户ID
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
