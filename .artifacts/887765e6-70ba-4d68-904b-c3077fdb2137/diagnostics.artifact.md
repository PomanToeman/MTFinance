# Diagnostics - CategoryFormViewModel Test Failure

## Observed Issue
Test `setMonthlyBudget_respectsMinimum` failed with:
`java.lang.AssertionError: expected:<0> but was:<1>`

This means `viewModel.getMonthlyBudget().getValue()` returned a value smaller than 50 (expected minimum). Specifically, it likely returned the input value 20, indicating that the clamping logic in `setMonthlyBudget` did not trigger or that the state was not correctly updated.

## Potential Causes

### 1. LiveData Inactivity in Tests
`Transformations.map` creates a LiveData that only updates its value when it is "active" (i.e., has an observer). In the test environment, if `viewModel.getMonthlyBudget()` is not observed, calling `getValue()` might return the initial value or a stale value.
- **Evidence**: `monthlyBudget` is a mapped LiveData. `successMessage` (which passes tests) is a `MutableLiveData`.
- **Solution**: Use `observeForever` on mapped LiveData in tests, or ensure they are properly triggered.

### 2. In-place modification of State Object
The ViewModel modifies the `CategoryFormFields` object in-place and then calls `setValue(fields)`. While `MutableLiveData` should still notify observers, this pattern can be risky and might lead to race conditions if multiple updates are queued.
- **Solution**: Implement a `copy()` method for `CategoryFormFields` and always set a new instance to the LiveData.

### 3. Asynchronous postValue in updateLiveData
The helper `updateLiveData` catches `RuntimeException` and falls back to `postValue`. In some test environments, `setValue` might throw if it touches certain Android components, forcing a fallback to `postValue`. `postValue` is always asynchronous (even with `InstantTaskExecutorRule`, it posts to a queue), so the subsequent line in the test might run before the LiveData update completes.
- **Solution**: Avoid `Looper` and provide a cleaner way to ensure synchronous updates in tests.

## Planned Fix
1. Add a `copy()` method to `CategoryFormFields`.
2. Update `CategoryFormViewModel` to use `postValue` only when on a non-main thread, and `setValue` otherwise, using `ArchTaskExecutor` to be thread-safe and test-friendly.
3. Update the test to ensure LiveData is observed.
