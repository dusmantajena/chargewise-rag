package com.dj.chargewise.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ChargewiseException {

    public ResourceNotFoundException(String resource, Object id) {
        super(HttpStatus.NOT_FOUND, "Resource not found", "%s '%s' was not found".formatted(resource, id));
    }
}
