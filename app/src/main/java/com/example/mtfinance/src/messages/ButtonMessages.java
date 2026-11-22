package com.example.mtfinance.src.messages;

/**
 * Modular message constants for button and action labels.
 */
public enum ButtonMessages implements MessageCli {
    SAVE("Save"),
    DELETE("Delete"),
    CLEAR("Clear"),
    BACK("Back"),
    ADD("Add"),
    IMPORT("Import"),
    SEARCH("Search"),
    EDIT("Edit"),
    SHOW_MORE("Show More"),
    CLOSE("Close"),
    CHOOSE_CATEGORY("Choose Category"),
    SET_PARENT("Set Parent"),
    RESET_TO_ROOT("Reset to Root"),
    PREV("<"),
    NEXT(">");

    private final String message;

    ButtonMessages(String message) {
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
