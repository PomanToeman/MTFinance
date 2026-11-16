package com.example.mtfinance.src.trackingengine;

/**
 * Custom exception thrown when a transaction business policy or mutation rule is violated.
 */
public class TransactionViolationException extends IllegalStateException {
    /**
     * Constructs a new TransactionViolationException with the specified detail message.
     * @param message the detail message
     */
    public TransactionViolationException(String message) {
        super(message);
    }

    /**
     * Constructs a new TransactionViolationException with the specified detail message and cause.
     * @param message the detail message
     * @param cause the cause
     */
    public TransactionViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}
