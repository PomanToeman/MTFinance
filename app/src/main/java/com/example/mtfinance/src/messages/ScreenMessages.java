package com.example.mtfinance.src.messages;

/**
 * Modular message constants for screen titles, headers, and general UI texts.
 */
public enum ScreenMessages implements MessageCli {
    CATEGORY_FORM_HEADER("Category Form"),
    TRANSACTION_LIST_HEADER("Transaction List"),
    TRANSACTION_DASHBOARD_HEADER("Transaction Dashboard"),
    TRANSACTION_FORM_HEADER("Transaction Form"),
    TRANSACTION_IMPORT_HEADER("Transaction Import");

    private final String message;

    ScreenMessages(String message) {
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
