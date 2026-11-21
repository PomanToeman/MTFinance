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
    IMPORT_CONFIGURATION_REQUIRED("Please configure required import headers and CSV file"),
    OPTIONAL_HEADER_DESCRIPTION_NOTE(""), OPTIONAL_HEADER_TYPE_NOTE("");

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