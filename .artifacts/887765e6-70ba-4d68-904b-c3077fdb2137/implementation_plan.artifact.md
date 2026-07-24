# Implementation Plan - Refactor CategoryFormViewModel for Background Execution

Refactor `CategoryFormViewModel` to perform database operations on background threads using an injected `Executor`, following the pattern established with `TransactionFormViewModel`.

## User Review Required

> [!IMPORTANT]
> - `CategoryFormViewModel` will use a consolidated `CategoryFormFields` state object internally to manage all form data.
> - Public `LiveData` getters will be maintained using `Transformations.map` to ensure future UI compatibility without breaking existing structure.
> - Background operations will use `postValue()` for thread-safe updates to LiveData.

## Proposed Changes

### [View Models]

#### [MODIFY] [CategoryFormViewModel.java](file:///C:/Users/Rebec/AndroidStudioProjects/MTFinance/MTFinance/app/src/main/java/com/example/mtfinance/src/viewmodels/CategoryFormViewModel.java)
- Update constructor to inject `java.util.concurrent.Executor`.
- Introduce a private static class `CategoryFormFields` to hold all form field data.
- Replace individual `MutableLiveData` for fields with a single `MutableLiveData<CategoryFormFields>`.
- Use `Transformations.map` to expose individual field `LiveData`.
- Refactor `loadCategoryForEditing()`, `clear()`, `saveCategory()`, and `deleteCategory()` to run on the `executor`.
- Implement `*Sync` versions of these methods for internal logic and testing.
- Ensure all LiveData updates from background threads use `postValue()`.

---

### [Tests]

#### [MODIFY] [CategoryFormViewModelTest.java](file:///C:/Users/Rebec/AndroidStudioProjects/MTFinance/MTFinance/app/src/test/java/com/example/mtfinance/src/viewmodels/CategoryFormViewModelTest.java)
- Provide a synchronous `Executor` (e.g., `Runnable::run`) to the `CategoryFormViewModel` constructor.
- Update `setUp()` to reflect the new constructor parameters.

## Verification Plan

### Automated Tests
- Run `CategoryFormViewModelTest` to ensure business logic (validation, budget constraints, hierarchy) remains correct.
- Execute `./gradlew :app:testDebugUnitTest` to run all unit tests in the project.

### Manual Verification
- Perform a clean build: `./gradlew clean :app:assembleDebug`.
- (Future) Verify on device once the UI screen for categories is implemented.
