package com.dj.chargewise.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.util.Map;

public class RateLimitExceededException extends ChargewiseException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(int capacity, long retryAfterSeconds) {
        super(HttpStatus.TOO_MANY_REQUESTS, "Too many requests",
                "Rate limit of %d dispute analyses exceeded. Try again in %d seconds."
                        .formatted(capacity, retryAfterSeconds));
        this.retryAfterSeconds = retryAfterSeconds;
    }

    @Override
    public Map<String, String> getHeaders() {
        return Map.of(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
    }
}
