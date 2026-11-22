package com.example.mtfinance.src.messages;

/**
 * Modular message constants for transaction domain operations and validations.
 */
public enum TransactionMessages implements MessageCli {
    TRANSACTION_UPDATED("Transaction Updated successfully"),
    TRANSACTION_SAVED("Transaction saved successfully"),
    TRANSACTION_DELETED("Transaction deleted successfully"),
    TRANSACTION_NOT_FOUND("Transaction not found."),
    TRANSACTION_SAVE_FAILED("Cannot save transaction: %s"),
    TRANSACTION_DELETE_NONE("No transaction to delete."),
    TRANSACTION_DELETE_FAILED("Failed to delete transaction: %s"),
    TRANSACTION_NAME_EMPTY("Name cannot be empty."),
    TRANSACTION_DATE_EMPTY("Date cannot be empty."),
    TRANSACTION_DATE_INVALID("Invalid date format. Accepted formats: yyyy-MM-dd or dd/MM/yyyy"),
    TRANSACTION_CATEGORY_EMPTY("Category cannot be empty."),
    TRANSACTION_CATEGORIES_NOT_EXIST("Some Categories do not exist."),
    TRANSACTION_DUPLICATE("Identical transaction already exists."),

    TRANSACTION_MIN_CATEGORY("Transaction must have at least one category"),
    TRANSACTION_TYPE_MISMATCH("Transaction and category types do not match.");

    private final String message;

    TransactionMessages(String message) {
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
