package com.achievement.service.impl;

import com.achievement.domain.vo.TypeCountVO;
import com.achievement.mapper.AchievementMainsMapper;
import com.achievement.service.AchievementStatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AchievementStatServiceImpl implements AchievementStatService {
    private final AchievementMainsMapper mainsMapper;
    @Override
    public List<TypeCountVO> typePie(Integer creatorId) {
        List<TypeCountVO> rows = mainsMapper.countByType(creatorId);
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows.stream()
                .filter(item -> item != null && item.getCount() != null && item.getCount() > 0)
                .toList();
    }
}
