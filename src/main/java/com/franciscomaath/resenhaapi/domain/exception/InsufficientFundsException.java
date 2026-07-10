package com.franciscomaath.resenhaapi.domain.exception;

import java.math.BigDecimal;

/**
 * Exception thrown when there are insufficient funds for an operation.
 * This exception should be used in wallet and transaction operations.
 */
public class InsufficientFundsException extends BusinessException {

    private BigDecimal required;
    private BigDecimal available;

    public InsufficientFundsException(String message) {
        super(message);
    }

    public InsufficientFundsException(String message, BigDecimal required, BigDecimal available) {
        super(String.format("%s. Required: %s, Available: %s", message, required, available));
        this.required = required;
        this.available = available;
    }

    public InsufficientFundsException(String message, Throwable cause) {
        super(message, cause);
    }

    public BigDecimal getRequired() {
        return required;
    }

    public BigDecimal getAvailable() {
        return available;
    }
}

