# Task List - Refactor ViewModels for Background Execution

- [x] Provide `Executor` in `databaseModule.java`
- [x] Refactor `TransactionFormViewModel.java`
    - [x] Create `TransactionFormFields` state class
    - [x] Refactor LiveData to use consolidated state
    - [x] Implement `*Sync` and `*Async` methods with `Executor`
- [x] Refactor `TransactionImportFormViewModel.java`
    - [x] Update constructor to inject `Executor`
    - [x] Refactor `readTransactionFile` and `importTransaction` to be asynchronous
- [x] Update Unit Tests
    - [x] `TransactionFormViewModelTest.java`
    - [x] `TransactionImportFormViewModelTest.java`
- [/] Verification
    - [ ] Clean build
    - [ ] Run all unit tests
    - [ ] Manual verification on device
