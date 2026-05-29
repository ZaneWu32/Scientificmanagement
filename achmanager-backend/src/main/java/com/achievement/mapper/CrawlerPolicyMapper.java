package com.achievement.mapper;

import com.achievement.domain.po.CrawlerPolicy;
import com.achievement.domain.vo.PolicyVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface CrawlerPolicyMapper extends BaseMapper<CrawlerPolicy> {

    List<PolicyVO> selectRelatedPolicies(@Param("achievementDocId") String achievementDocId,
                                          @Param("limit") int limit);

    List<Map<String, Object>> countGroupByCrawlerId();

    List<Map<String, Object>> selectLightweightPolicies();

    List<PolicyVO> selectPolicyDetailByIds(@Param("ids") List<Long> ids);
}
