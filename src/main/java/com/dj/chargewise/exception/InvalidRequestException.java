package com.dj.chargewise.exception;

import org.springframework.http.HttpStatus;

public class InvalidRequestException extends ChargewiseException {

    public InvalidRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, "Invalid request", message);
    }
}
