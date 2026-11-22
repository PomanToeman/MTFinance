package com.example.mtfinance.src.messages;

/**
 * Modular message constants for CSV import operations and headers.
 */
public enum ImportMessages implements MessageCli {
    NO_FILE_FOUND("No file found"),
    IMPORT_STOPPED("Seems import stopped working."),
    IMPORT_DATE_FORMAT_INVALID("Invalid Date Formatter: %s"),
    IMPORT_FILE_INVALID("Invalid Transaction File: %s"),
    IMPORT_FILE_READ_FAILED("Couldn't read transaction File: %s"),
    IMPORT_SUCCESS("%d Transaction/s successfully imported!"),
    IMPORT_FAILED("%d Transaction/s failed to import!"),
    IMPORT_NAME_HEADER_MISSING("Name header is missing or not valid"),
    IMPORT_DATE_HEADER_MISSING("Date header is missing or not valid"),
    IMPORT_AMOUNT_HEADER_MISSING("Amount header is missing or not valid"),
    IMPORT_PARSER_MISSING("CSV Parser is not initialized"),
    IMPORT_TYPE_HEADER_MISSING("Type header is invalid"),
    IMPORT_DATE_FORMAT_MISSING("Date Formatter is missing");

    private final String message;

    ImportMessages(String message) {
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
