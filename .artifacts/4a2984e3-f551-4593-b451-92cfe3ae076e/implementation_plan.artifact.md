# Implementation Plan - Revert Threading Changes

Revert all changes made to introduce threading in ViewModels and their tests. This will return the codebase to its original synchronous state for these components.

## Proposed Changes

### [Component: ViewModels]

#### [MODIFY] [TransactionFormViewModel.java](file:///C:/Users/Rebec/AndroidStudioProjects/MTFinance/MTFinance/app/src/main/java/com/example/mtfinance/src/viewmodels/TransactionFormViewModel.java)
- Revert `setIsLoading`, `setErrorMessage`, `setSuccessMessage` to use `setValue()`.
- Revert `loadTransactionForEditing()` to be synchronous and remove `loadTransactionForEditingSync()`.
- Revert `clear()` to use `setValue()`.
- Revert `saveTransaction()` to be synchronous and remove `saveTransactionSync()`.
- Revert `deleteTransaction()` to be synchronous and remove `deleteTransactionSync()`.

#### [MODIFY] [CategoryFormViewModel.java](file:///C:/Users/Rebec/AndroidStudioProjects/MTFinance/MTFinance/app/src/main/java/com/example/mtfinance/src/viewmodels/CategoryFormViewModel.java)
- Revert all private setters to use `setValue()`.
- Revert `setName`, `setDescription`, `setParentId`, `setMonthlyBudget` to use `setValue()`.
- Revert `loadCategoryForEditing()` to be synchronous and remove `loadCategoryForEditingSync()`.
- Revert `clear()` to use `setValue()`.
- Revert `saveCategory()` to be synchronous and remove `saveCategorySync()`.
- Revert `deleteCategory()` to be synchronous and remove `deleteCategorySync()`.
- Remove `setEditCategorySync()`.

#### [MODIFY] [TransactionImportFormViewModel.java](file:///C:/Users/Rebec/AndroidStudioProjects/MTFinance/MTFinance/app/src/main/java/com/example/mtfinance/src/viewmodels/TransactionImportFormViewModel.java)
- Revert all private setters to use `setValue()`.
- Revert `clear()` to use `setValue()`.
- Revert `readTransactionFile()` to be synchronous and remove `readTransactionFileSync()`.
- Revert `importTransaction()` to be synchronous and remove `importTransactionSync()`.

### [Component: Tests]

#### [MODIFY] [TransactionFormViewModelTest.java](file:///C:/Users/Rebec/AndroidStudioProjects/MTFinance/MTFinance/app/src/test/java/com/example/mtfinance/src/viewmodels/TransactionFormViewModelTest.java)
- Revert all `*Sync` method calls back to original names.
- Ensure all `verify` calls are back to original state (no `timeout`).

#### [MODIFY] [TransactionImportFormViewModelTest.java](file:///C:/Users/Rebec/AndroidStudioProjects/MTFinance/MTFinance/app/src/test/java/com/example/mtfinance/src/viewmodels/TransactionImportFormViewModelTest.java)
- Revert all `*Sync` method calls back to original names.

#### [MODIFY] [CategoryFormViewModelTest.java](file:///C:/Users/Rebec/AndroidStudioProjects/MTFinance/MTFinance/app/src/test/java/com/example/mtfinance/src/viewmodels/CategoryFormViewModelTest.java)
- Revert all `*Sync` method calls back to original names.

## Verification Plan

### Automated Tests
- Run all unit tests in the `viewmodels` package.
- Command: `./gradlew :app:testDebugUnitTest --tests "com.example.mtfinance.src.viewmodels.*"`
