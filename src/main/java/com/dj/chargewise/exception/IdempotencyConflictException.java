package com.dj.chargewise.exception;

import org.springframework.http.HttpStatus;

public class IdempotencyConflictException extends ChargewiseException {

    public IdempotencyConflictException(String message) {
        super(HttpStatus.CONFLICT, "Idempotency conflict", message);
    }
}
