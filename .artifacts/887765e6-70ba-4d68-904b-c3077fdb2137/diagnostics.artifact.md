# Diagnostics - CategoryFormViewModel Test Failures (Take 2)

## 1. saveCategory_withParent_setsParentId Failure
**Issue**: `trackingRepository.insertCategory` was not invoked.
**Cause**: In the new background refactor, `CategoryFormFields` defaulted `monthlyBudget` to `BigDecimal.ZERO`. The `Category` constructor calls `TrackingUtlis.checkAmount(monthlyBudget)`, which throws an `IllegalArgumentException` if the amount is zero. This exception was caught in the `saveCategorySync` catch block, preventing the repository call.
**Fix**: Updated `CategoryFormFields` defaults to match the original ViewModel (Name="Name", Budget=1).

## 2. setMonthlyBudget_respectsMinimum Failure
**Issue**: `AssertionError: expected:<0> but was:<1>` (Actual value was 0/default instead of the set value 20).
**Cause**: This indicates a race condition or propagation failure. Since `postValue` is used for background updates, calling `getValue()` immediately in a test might return the old value. Even with `InstantTaskExecutorRule`, `postValue` is posted to a queue.
**Fix**:
- Use `setValue` whenever on the main thread (detected via `Looper` or `ArchTaskExecutor`).
- Ensure all tests observe the LiveData they are asserting on (already added to `setUp`).
- Restored original logic where budget clamping is performed immediately in the setter if possible, or ensured validation handles it correctly.

## 3. General "Bugs" reported by User
The transition to a single state object with `postValue` caused tests and potentially the UI to see stale data. By providing better defaults and ensuring synchronous updates on the main thread, the ViewModel will behave predictably in both tests and production.
