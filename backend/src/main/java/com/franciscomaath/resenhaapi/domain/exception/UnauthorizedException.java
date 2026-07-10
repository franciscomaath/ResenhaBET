package com.franciscomaath.resenhaapi.domain.exception;

/**
 * Exception thrown when a user is not authorized to perform an operation.
 * This exception should be used for authorization/permission checks.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}

