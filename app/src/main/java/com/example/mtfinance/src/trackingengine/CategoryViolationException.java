package com.example.mtfinance.src.trackingengine;

/**
 * Custom exception thrown when a category business policy or hierarchy rule is violated.
 */
public class CategoryViolationException extends IllegalStateException {
    /**
     * Constructs a new CategoryViolationException with the specified detail message.
     * @param message the detail message
     */
    public CategoryViolationException(String message) {
        super(message);
    }

    /**
     * Constructs a new CategoryViolationException with the specified detail message and cause.
     * @param message the detail message
     * @param cause the cause
     */
    public CategoryViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}
