package com.achievement.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LlmUsage {

    DEFAULT("default");

    private final String value;
}
