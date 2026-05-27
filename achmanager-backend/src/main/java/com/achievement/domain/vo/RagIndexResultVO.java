package com.achievement.domain.vo;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class RagIndexResultVO {
    private int total;
    private int success;
    private int failed;
    private List<String> failedIds = new ArrayList<>();
}
