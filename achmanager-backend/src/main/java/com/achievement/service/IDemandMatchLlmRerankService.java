package com.achievement.service;

import java.util.List;

import com.achievement.domain.po.DemandItem;
import com.achievement.domain.po.DemandMatch;

public interface IDemandMatchLlmRerankService {

    List<DemandMatch> rerank(DemandItem demand, List<DemandMatch> candidates, double maxEsScore);
}
