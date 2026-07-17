# Walkthrough - Refactored Hard-coded Strings to MessageCli Enums

I have refactored all hard-coded success messages, error messages, and exception strings across the ViewModels and Repositories into the `MessageCli` enum. This centralizes message management and improves scalability.

## Changes Made

### [MessageCli.java](file:///C:/Users/Rebec/AndroidStudioProjects/MTFinance/MTFinance/app/src/main/java/com/example/mtfinance/src/MessageCli.java)
- **Centralized Messages**: Expanded the enum to include over 30 unique messages for Categories, Transactions, and Imports.
- **Dynamic Formatting**: Optimized the `getMessage` method to support `Object... args` and `String.format`, allowing for dynamic messages like `IMPORT_SUCCESS` ("%d Transaction/s successfully imported!").

### [ViewModels]
- **[CategoryFormViewModel.java](file:///C:/Users/Rebec/AndroidStudioProjects/MTFinance/MTFinance/app/src/main/java/com/example/mtfinance/src/viewmodels/CategoryFormViewModel.java)**: Replaced all UI messages and validation exceptions with `MessageCli` constants.
- **[TransactionFormViewModel.java](file:///C:/Users/Rebec/AndroidStudioProjects/MTFinance/MTFinance/app/src/main/java/com/example/mtfinance/src/viewmodels/TransactionFormViewModel.java)**: Migrated all user feedback and exception strings to the central enum.
- **[TransactionImportFormViewModel.java](file:///C:/Users/Rebec/AndroidStudioProjects/MTFinance/MTFinance/app/src/main/java/com/example/mtfinance/src/viewmodels/TransactionImportFormViewModel.java)**: Refactored the import logic. Specifically, replaced previously empty `IllegalArgumentException` strings with descriptive `MessageCli` enums (e.g., `IMPORT_NAME_HEADER_MISSING`).

### [Core & Repositories]
- **[TrackingRepository.java](file:///C:/Users/Rebec/AndroidStudioProjects/MTFinance/MTFinance/app/src/main/java/com/example/mtfinance/src/repositories/TrackingRepository.java)**: Updated `deleteRelationship` to use `MessageCli.TRANSACTION_MIN_CATEGORY`.
- **[TrackingUtlis.java](file:///C:/Users/Rebec/AndroidStudioProjects/MTFinance/MTFinance/app/src/main/java/com/example/mtfinance/src/trackingengine/TrackingUtlis.java)**: Updated `checkAmount` to use `MessageCli.INVALID_AMOUNT`.

## Verification Results

### Automated Tests
- **Unit Tests**: All 70 unit tests in the `:app` module passed successfully.
  - **Command**: `./gradlew :app:testDebugUnitTest`
  - **Result**: `70 passed, 0 skipped, 0 failed`
- **Updated Tests**: Updated `TransactionImportFormViewModelTest.java` to use `MessageCli` for its assertions, ensuring consistency between the implementation and its verification.
