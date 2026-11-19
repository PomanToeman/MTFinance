package com.example.mtfinance.src.trackingengine;

/**
 * Custom exception thrown when a system/settings preference rule or validation is violated.
 */
public class SettingsPreferenceViolationException extends IllegalStateException {

    /**
     * Constructs a new SettingsPreferenceViolationException with the specified detail message.
     * @param message the detail message
     */
    public SettingsPreferenceViolationException(String message) {
        super(message);
    }

    /**
     * Constructs a new SettingsPreferenceViolationException with the specified detail message and cause.
     * @param message the detail message
     * @param cause the cause
     */
    public SettingsPreferenceViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}
