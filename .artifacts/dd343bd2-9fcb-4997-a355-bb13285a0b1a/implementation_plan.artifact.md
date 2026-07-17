# Implementation Plan - Test Optional Type Header and Date-Time Parsing

This plan adds test cases for the optional `typeHeader` and `dateFormatter` (with time support) in `TransactionImportFormViewModel`, and fixes identified bugs in their implementation.

## Proposed Changes

### [ViewModels]

#### [MODIFY] [TransactionImportFormViewModel.java](file:///C:/Users/Rebec/AndroidStudioProjects/MTFinance/MTFinance/app/src/main/java/com/example/mtfinance/src/viewmodels/TransactionImportFormViewModel.java)

1.  **Fix `importTransaction`**: Pass the value from the record to `determineType`, not the header name.
2.  **Fix `determineType`**: Change parameter name from `typeHeader` to `typeValue` for clarity.
3.  **Enhance Date Parsing**: Update the loop to attempt parsing as `LocalDateTime` if `LocalDate` fails, or based on the format pattern, to support CSVs with date and time.

#### [MODIFY] [TransactionImportFormViewModelTest.java](file:///C:/Users/Rebec/AndroidStudioProjects/MTFinance/MTFinance/app/src/test/java/com/example/mtfinance/src/viewmodels/TransactionImportFormViewModelTest.java)

1.  **`importTransaction_withTypeHeader_usesTypeFromCsv`**:
    *   Create CSV with "Date", "Name", "Amount", "Type".
    *   Set `typeHeader` to "Type".
    *   Verify that `TrackingType.fromString` is respected (e.g., "Credit" mapped to `INCOME`).
2.  **`importTransaction_withDateTime_parsesCorrectly`**:
    *   Create CSV with date and time (e.g., "16/07/2026 14:30").
    *   Set `dateFormatter` to "dd/MM/yyyy HH:mm".
    *   Verify the transaction is saved with the correct date and time.

## Verification Plan

### Automated Tests
- Run `TransactionImportFormViewModelTest` (Unit Test):
  `./gradlew :app:testDebugUnitTest --tests "com.example.mtfinance.src.viewmodels.TransactionImportFormViewModelTest"`

### Manual Verification
- None required as these are unit tests.
