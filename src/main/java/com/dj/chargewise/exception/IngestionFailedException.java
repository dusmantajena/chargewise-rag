package com.dj.chargewise.exception;

import org.springframework.http.HttpStatus;

public class IngestionFailedException extends ChargewiseException {

    public IngestionFailedException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "Ingestion failed", message);
    }

    public IngestionFailedException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "Ingestion failed", message, cause);
    }
}
