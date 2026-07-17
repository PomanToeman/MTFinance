# Implementation Plan - Refactor Hard-coded Strings to MessageCli Enums

Refactor all success/error messages and exception strings across the ViewModels and Repositories into the `MessageCli` enum for better maintainability and scalability.

## Proposed Changes

### [MTFinance Core]

#### [MODIFY] [MessageCli.java](file:///C:/Users/Rebec/AndroidStudioProjects/MTFinance/MTFinance/app/src/main/java/com/example/mtfinance/src/MessageCli.java)
Update the enum to include all identified strings, categorized logically.

```java
public enum MessageCli {
    // General & Import
    NO_FILE_FOUND("No file found"),
    FORM_INCOMPLETE("Form is not complete: %s"),
    IMPORT_STOPPED("Seems import stopped working."),
    IMPORT_DATE_FORMAT_INVALID("Invalid Date Formatter: %s"),
    IMPORT_FILE_INVALID("Invalid Transaction File: %s"),
    IMPORT_FILE_READ_FAILED("Couldn't read transaction File: %s"),
    IMPORT_SUCCESS("%d Transaction/s successfully imported!"),
    IMPORT_FAILED("%d Transaction/s failed to import!"),
    IMPORT_NAME_HEADER_MISSING("Name header is missing"),
    IMPORT_DATE_HEADER_MISSING("Date header is missing"),
    IMPORT_AMOUNT_HEADER_MISSING("Amount header is missing"),
    IMPORT_PARSER_MISSING("CSV Parser is not initialized"),

    // Category
    CATEGORY_DELETED("Category deleted successfully"),
    CATEGORY_SAVED("Category saved successfully"),
    CATEGORY_NOT_FOUND("Cannot find category"),
    CATEGORY_ROOT_EDIT_DENIED("cannot edit root categories"),
    CATEGORY_DELETE_NONE("No category to delete"),
    CATEGORY_DELETE_FAILED("Cannot delete category: %s"),
    CATEGORY_SAVE_FAILED("Cannot save category: %s"),
    CATEGORY_NAME_EMPTY("Name cannot be empty"),
    CATEGORY_NAME_EXISTS("Category with this name already exists"),
    CATEGORY_PARENT_NOT_FOUND("Parent category does not exist"),

    // Transaction
    TRANSACTION_UPDATED("Transaction Updated successfully"),
    TRANSACTION_SAVED("Transaction saved successfully"),
    TRANSACTION_DELETED("Transaction deleted successfully"),
    TRANSACTION_NOT_FOUND("Transaction not found."),
    TRANSACTION_SAVE_FAILED("Cannot save transaction: %s"),
    TRANSACTION_DELETE_NONE("No transaction to delete."),
    TRANSACTION_DELETE_FAILED("Failed to delete transaction: %s"),
    TRANSACTION_NAME_EMPTY("Name cannot be empty."),
    TRANSACTION_CATEGORY_EMPTY("Category cannot be empty."),
    TRANSACTION_CATEGORIES_NOT_EXIST("Some Categories do not exist."),
    TRANSACTION_DUPLICATE("Identical transaction already exists."),
    TRANSACTION_MIN_CATEGORY("Transaction must have at least one category"),

    // Utils
    INVALID_AMOUNT("Invalid amount: %s");
    // ... rest of the implementation
}
```

### [ViewModels]

#### [MODIFY] [CategoryFormViewModel.java](file:///C:/Users/Rebec/AndroidStudioProjects/MTFinance/MTFinance/app/src/main/java/com/example/mtfinance/src/viewmodels/CategoryFormViewModel.java)
Replace hard-coded strings with `MessageCli` constants.

#### [MODIFY] [TransactionFormViewModel.java](file:///C:/Users/Rebec/AndroidStudioProjects/MTFinance/MTFinance/app/src/main/java/com/example/mtfinance/src/viewmodels/TransactionFormViewModel.java)
Replace hard-coded strings with `MessageCli` constants.

#### [MODIFY] [TransactionImportFormViewModel.java](file:///C:/Users/Rebec/AndroidStudioProjects/MTFinance/MTFinance/app/src/main/java/com/example/mtfinance/src/viewmodels/TransactionImportFormViewModel.java)
Replace hard-coded strings with `MessageCli` constants, including filling empty strings in `validateImport()`.

### [Repositories & Utils]

#### [MODIFY] [TrackingRepository.java](file:///C:/Users/Rebec/AndroidStudioProjects/MTFinance/MTFinance/app/src/main/java/com/example/mtfinance/src/repositories/TrackingRepository.java)
Update `deleteRelationship` exception message.

#### [MODIFY] [TrackingUtlis.java](file:///C:/Users/Rebec/AndroidStudioProjects/MTFinance/MTFinance/app/src/main/java/com/example/mtfinance/src/trackingengine/TrackingUtlis.java)
Update `checkAmount` exception message.

## Verification Plan

### Automated Tests
- Run all unit tests to ensure that the logic still holds and no regression is introduced (especially around exception handling and message setting).
  `./gradlew :app:testDebugUnitTest`

### Manual Verification
- Since this is a refactor of strings, I will verify that the project builds successfully and the tests pass.
