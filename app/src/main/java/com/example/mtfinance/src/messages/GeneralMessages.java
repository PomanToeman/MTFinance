package com.example.mtfinance.src.messages;

/**
 * Modular message constants for general application operations and helpers.
 */
public enum GeneralMessages implements MessageCli {
    FORM_INCOMPLETE("Form is not complete: %s"),
    INVALID_AMOUNT("Invalid amount: %s"),
    DELETE_BUTTON_CONFIRMATION("Are you sure you want to delete %s?");

    private final String message;

    GeneralMessages(String message) {
        this.message = message;
    }

    @Override
    public String getMessage(Object... args) {
        if (args.length > 0) {
            return String.format(message, args);
        }
        return message;
    }
}
