package com.achievement.exception;

import lombok.Getter;

@Getter
public class LlmException extends RuntimeException {

    private final Integer errorCode;

    public LlmException(String message) {
        super(message);
        this.errorCode = null;
    }

    public LlmException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }

    public LlmException(Integer errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public LlmException(Integer errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
