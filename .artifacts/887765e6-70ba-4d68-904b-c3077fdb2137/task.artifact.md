# Task List - Support Uri in TransactionImportFormViewModel

- [x] Modify `TransactionImportFormViewModel.java`
    - [x] Update constructor to inject `Application`
    - [x] Add `fileUri` state and public getters/setters
    - [x] Refactor `readTransactionFileSync` to support `Uri` input streams
    - [x] Update `clear` to reset `fileUri`
- [x] Update `TransactionImportFormViewModelTest.java`
    - [x] Mock `Application` and `ContentResolver`
    - [x] Add test for loading headers from a `Uri`
- [x] Verification
    - [x] Run all unit tests
    - [x] Clean build
