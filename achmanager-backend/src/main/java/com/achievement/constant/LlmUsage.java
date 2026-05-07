package com.achievement.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LlmUsage {

    DEFAULT("default"),
    REPORT("report");

    private final String value;
}
