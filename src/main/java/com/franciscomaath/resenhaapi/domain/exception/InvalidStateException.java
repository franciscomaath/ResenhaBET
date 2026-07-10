package com.franciscomaath.resenhaapi.domain.exception;

/**
 * Exception thrown when an operation cannot be performed due to invalid entity state.
 * This exception should be used when an entity is in a state that doesn't allow the requested operation.
 */
public class InvalidStateException extends RuntimeException {

    public InvalidStateException(String message) {
        super(message);
    }

    public InvalidStateException(String message, Throwable cause) {
        super(message, cause);
    }
}

