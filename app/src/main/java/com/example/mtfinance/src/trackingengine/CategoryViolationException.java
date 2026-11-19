package com.example.mtfinance.src.trackingengine;

/**
 * Unchecked exception thrown when a user action or category business policy rule is violated.
 *
 * Distinction from {@link CategoryStateException}:
 * <ul>
 *   <li><b>{@link CategoryViolationException}</b>: Thrown for user-triggered domain rule violations
 *       (e.g., self-parenting, tracking type mismatch, descendant as parent, max depth exceeded).</li>
 *   <li><b>{@link CategoryStateException}</b>: Thrown for impossible internal app or cache invariant breakages
 *       (e.g., incomplete cache with {@code parentId != null} and {@code parent == null}).</li>
 * </ul>
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
