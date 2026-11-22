package com.example.mtfinance.src.messages;

/**
 * Modular message constants for general application operations and helpers.
 */
public enum GeneralMessages implements MessageCli {
    FORM_INCOMPLETE("Form is not complete: %s"),
    INVALID_AMOUNT("Invalid amount: %s"),
    DELETE_BUTTON_CONFIRMATION("Are you sure you want to delete %s?"),
    NON_CSV_FILE_READ("Only CSV files (.csv) are supported"),
    STREAM_OPEN_FAILED("Could not open stream from Uri"),
    INVALID_TYPE_ERROR_MESSAGE("Type cannot be empty or invalid."),
    IMPORT_CONFIGURATION_REQUIRED("Please configure required import headers and CSV file"),
    OPTIONAL_HEADER_DESCRIPTION_NOTE("Optional: If left blank, transaction description defaults to empty"),
    OPTIONAL_HEADER_TYPE_NOTE("Optional: If left blank, transaction type is automatically determined by amount sign");

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