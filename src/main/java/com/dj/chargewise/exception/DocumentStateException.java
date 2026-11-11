package com.dj.chargewise.exception;

import org.springframework.http.HttpStatus;

public class DocumentStateException extends ChargewiseException {

    public DocumentStateException(String message) {
        super(HttpStatus.CONFLICT, "Document state conflict", message);
    }
}
