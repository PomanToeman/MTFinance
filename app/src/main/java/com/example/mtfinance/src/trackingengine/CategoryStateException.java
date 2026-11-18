package com.example.mtfinance.src.trackingengine;

/**
 * Unchecked exception thrown when an impossible internal app state or broken cache invariant is detected.
 *
 * Distinction from {@link CategoryViolationException}:
 * <ul>
 *   <li><b>{@link CategoryViolationException}</b>: Thrown when a user action or domain operation violates business rules
 *       (e.g., self-parenting, tracking type mismatch, descendant as parent, max depth exceeded).</li>
 *   <li><b>{@link CategoryStateException}</b>: Thrown when an unrecoverable, impossible internal app or cache state is detected
 *       (e.g., a non-null {@code parentId} exists with a {@code null} cached parent reference).</li>
 * </ul>
 */
public class CategoryStateException extends IllegalStateException {

    /**
     * Constructs a new CategoryStateException with the specified detail message.
     * @param message the detail message
     */
    public CategoryStateException(String message) {
        super(message);
    }

    /**
     * Constructs a new CategoryStateException with the specified detail message and cause.
     * @param message the detail message
     * @param cause the cause
     */
    public CategoryStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
