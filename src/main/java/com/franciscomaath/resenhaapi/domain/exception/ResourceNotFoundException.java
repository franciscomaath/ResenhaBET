package com.franciscomaath.resenhaapi.domain.exception;

/**
 * Exception thrown when a requested resource is not found.
 * This exception should be used when an entity cannot be found by its ID or other identifiers.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: %s", resourceName, fieldName, fieldValue));
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

