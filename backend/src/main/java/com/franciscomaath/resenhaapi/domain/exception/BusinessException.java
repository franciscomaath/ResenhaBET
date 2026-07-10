package com.franciscomaath.resenhaapi.domain.exception;

/**
 * Exception thrown when a business rule is violated.
 * This exception should be used for validation errors and business logic violations.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}

