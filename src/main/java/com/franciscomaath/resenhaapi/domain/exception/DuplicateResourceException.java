package com.franciscomaath.resenhaapi.domain.exception;

/**
 * Exception thrown when a duplicate resource is being created or modified.
 * This exception should be used when attempting to create/modify an entity that already exists.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s already exists with %s: %s", resourceName, fieldName, fieldValue));
    }

    public DuplicateResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}

