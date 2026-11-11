package com.dj.chargewise.exception;

import org.springframework.http.HttpStatus;

public class InsufficientContextException extends ChargewiseException {

    public InsufficientContextException(String reason) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "Insufficient context", reason);
    }
}
