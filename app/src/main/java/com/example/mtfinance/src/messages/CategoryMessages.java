package com.example.mtfinance.src.messages;

/**
 * Modular message constants for category domain operations and validations.
 */
public enum CategoryMessages implements MessageCli {
    CATEGORY_DELETED("Category deleted successfully"),
    CATEGORY_SAVED("Category saved successfully"),
    CATEGORY_NOT_FOUND("Cannot find category"),
    CATEGORY_ROOT_EDIT_DENIED("Cannot edit root categories"),
    CATEGORY_DELETE_NONE("No category to delete"),
    CATEGORY_DELETE_FAILED("Cannot delete category: %s"),
    CATEGORY_SAVE_FAILED("Cannot save category: %s"),
    CATEGORY_NAME_EMPTY("Name cannot be empty"),
    CATEGORY_NAME_EXISTS("Category with this name already exists"),
    CATEGORY_PARENT_NOT_FOUND("Parent category does not exist"),
    CATEGORY_MAX_DEPTH_REACHED("Cannot exceed Maximum Depth of %d"),
    BELOW_MINIMUM_BUDGET("$%s is below minimum budget of $%s"),
    CATEGORY_PARENT_SELF("Cannot set parent as self"),
    CATEGORY_SELECTED("Category selected: %s"),
    BUDGET_DISPLAY("Budget: %s"),
    TYPE_DISPLAY("Type: %s"),
    NO_PARENT_FOUND("No parent found"),
    NO_SUB_CATEGORIES("No sub-categories"),
    NO_TRANSACTIONS("No transactions"),
    CATEGORY_DESCRIPTION("Description: %s"),
    CATEGORY_MONTHLY_BUDGET("Monthly Budget: $%s");

    private final String message;

    CategoryMessages(String message) {
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
