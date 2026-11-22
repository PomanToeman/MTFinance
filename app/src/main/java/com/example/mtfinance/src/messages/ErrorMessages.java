package com.example.mtfinance.src.messages;

/**
 * Modular message constants for application errors, exception formatters, and system violations.
 */
public enum ErrorMessages implements MessageCli {

    FAILED_SAVE_SETTINGS("Failed to save settings: %s"),
    PREFERENCE_VIOLATION("Preference configuration violation occurred. Changes rolled back: %s"),
    INVALID_START_DAY("Invalid start day of month value: %s"),
    START_DAY_EMPTY("Start day of month cannot be empty."),
    START_DAY_RANGE("Start day of month must be an integer between 1 and 31."),
    FAILED_DELETE_APP_DATA("Failed to delete app data: %s"),
    FAILED_DELETE_TRANSACTIONS("Failed to delete transactions: %s"),
    INVALID_TRANSACTION_TYPE("Invalid tracking type. Valid types are EXPENSE, INCOME, ACCOUNT_TRANSFERS."),
    TYPE_CANNOT_BE_EMPTY("Type cannot be empty or invalid."),
    HEADER_DOES_NOT_EXIST("Header '%s' does not exist in CSV file."),
    MIN_REQUIRED_COLUMNS("CSV file must contain at least %s columns (Name, Amount, Date)."),
    DEFAULT_CATEGORY_INIT_FAILED("Default category initialization note: %s"),
    PREFERENCES_READ_FALLBACK("DAO preferences read fallback: %s"),
    DEFAULT_CATEGORY_INIT_NOTICE("Default category initialization notice: %s"),
    DEFAULT_SYSTEM_PREFERENCE_NOTICE("Default system preference initialization notice: %s"),
    CATEGORY_DELETION_WIPE_NOTICE("Category deletion notice during wipe: %s"),
    ROOT_CATEGORY_RESOLUTION_NOTICE("Root category resolution notice: %s"),
    IMPORT_ROW_FAILED_NOTICE("Failed to import CSV record '%s': %s");

    private final String message;

    ErrorMessages(String message) {
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
