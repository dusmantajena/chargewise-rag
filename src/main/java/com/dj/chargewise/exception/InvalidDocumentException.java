package com.dj.chargewise.exception;

import org.springframework.http.HttpStatus;

public class InvalidDocumentException extends ChargewiseException {

    public InvalidDocumentException(String message) {
        super(HttpStatus.BAD_REQUEST, "Invalid document", message);
    }
}
