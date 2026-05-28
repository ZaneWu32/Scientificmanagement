package com.achievement.domain.vo;

import java.util.List;

import lombok.Data;

@Data
public class AutoFillResultVO {

    private String fileName;
    private String recognizedAt;
    private List<AutoFillFieldVO> fields;
    private List<String> pendingConfirmations;

    @Data
    public static class AutoFillFieldVO {
        private String key;
        private String label;
        private String value;
        private double confidence;
        private String sourceSnippet;
        private boolean needsConfirm;
    }
}
