package com.dj.chargewise.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public abstract class ChargewiseException extends RuntimeException {

    private final HttpStatus status;
    private final String title;

    protected ChargewiseException(HttpStatus status, String title, String message) {
        super(message);
        this.status = status;
        this.title = title;
    }

    protected ChargewiseException(HttpStatus status, String title, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.title = title;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    public Map<String, Object> getProperties() {
        return Map.of();
    }

    public Map<String, String> getHeaders() {
        return Map.of();
    }
}
