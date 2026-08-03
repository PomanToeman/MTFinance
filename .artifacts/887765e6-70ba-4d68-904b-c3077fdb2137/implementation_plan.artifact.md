# Implementation Plan - Fix Category List Update and Budget Comparison Bugs

Address two critical issues:
1. Visual bug where parent categories don't update their budgets in the list when children change.
2. Logic bug where budget cannot be set exactly equal to the minimum budget.

## User Review Required

> [!IMPORTANT]
> - **Equality Logic Change**: I will modify `Category.equals()` and `hashCode()` to include all display fields (`name`, `description`, `monthlyBudget`). This is crucial for Jetpack Compose to detect state changes in the `LazyColumn` and trigger recomposition.
> - **Budget Clamping Fix**: I will change the comparison logic in `Category.java` to allow setting the budget exactly at the minimum threshold.

## Proposed Changes

### [Tracking Engine]

#### [MODIFY] [Category.java](file:///C:/Users/Rebec/AndroidStudioProjects/MTFinance/MTFinance/app/src/main/java/com/example/mtfinance/src/trackingengine/Category.java)
- **Fix Budget Comparison**: Change `setMonthlyBudget(BigDecimal)` to use `>=` when comparing with the minimum budget.
- **Improve Equality**: Update `equals(Object o)` and `hashCode()` to include `name`, `description`, `monthlyBudget`, and `parentId`. This ensures that `CategoryWithTransactions` (which delegates equality to Category) correctly signals a change to the UI.

#### [MODIFY] [CategoryWithTransactions.java](file:///C:/Users/Rebec/AndroidStudioProjects/MTFinance/MTFinance/app/src/main/java/com/example/mtfinance/src/trackingengine/CategoryWithTransactions.java)
- Ensure `equals()` and `hashCode()` correctly reflect changes in the underlying `category` object.

---

### [Repositories]

#### [MODIFY] [CategoryRepository.java](file:///C:/Users/Rebec/AndroidStudioProjects/MTFinance/MTFinance/app/src/main/java/com/example/mtfinance/src/repositories/CategoryRepository.java)
- **Tree Updates**: Refactor `updateCategoryTree(Category)` to ensure that all ancestors (including the system root) whose budgets are modified by propagation are consistently collected and persisted.

---

### [Database]

#### [MODIFY] [CategoryDao.java](file:///C:/Users/Rebec/AndroidStudioProjects/MTFinance/MTFinance/app/src/main/java/com/example/mtfinance/src/repositories/roomdatabase/CategoryDao.java)
- Ensure `updateAll` is atomic. If needed, I will wrap it in a `@Transaction` method to ensure that all category updates in a tree are emitted as a single `LiveData` change.

## Verification Plan

### Automated Tests
- **New Test**: `setMonthlyBudget_atExactMinimum_succeeds()` to verify the comparison fix.
- **New Test**: `updateChildBudget_updatesParentBudgetInRepository()` to verify propagation and persistence logic.
- Run all unit tests to ensure no regressions in validation or hierarchy depth checks.

### Manual Verification
1. Navigate to Category List.
2. Note a Parent category's current minimum budget (sum of its sub-categories).
3. Try to set its budget exactly to that value. Verify it saves.
4. Increase a sub-category's budget so that the parent's minimum budget now exceeds its set budget.
5. Save and return to the list. Verify the parent's budget is updated visually.
