package com.dj.chargewise.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.util.Map;

public class LlmUnavailableException extends ChargewiseException {

    private final Duration retryAfter;

    public LlmUnavailableException(String message, Duration retryAfter, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "AI service unavailable", message, cause);
        this.retryAfter = retryAfter;
    }

    @Override
    public Map<String, String> getHeaders() {
        return Map.of(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter.toSeconds()));
    }
}
