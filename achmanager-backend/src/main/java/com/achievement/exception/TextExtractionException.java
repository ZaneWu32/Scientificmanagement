package com.achievement.exception;

import lombok.Getter;

@Getter
public class TextExtractionException extends RuntimeException {

    public TextExtractionException(String message) {
        super(message);
    }

    public TextExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
