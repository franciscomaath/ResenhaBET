package com.franciscomaath.resenhaapi.domain.exception;

/**
 * Exception thrown when input validation fails.
 * This exception should be used for data validation errors at the domain level.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

