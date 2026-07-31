# Task List - Fix Category List and Budget Bugs

- [x] Fix logic in `Category.java`
    - [x] Update `setMonthlyBudget` comparison to `>=`
    - [x] Refactor `equals()` and `hashCode()` to include display fields
    - [x] Fix `makeChildrenCongruent()` iteration bug
- [x] Update `CategoryWithTransactions.java`
    - [x] Ensure `equals()` and `hashCode()` correctly reflect category changes
- [x] Improve `CategoryRepository.java`
    - [x] Refactor `updateCategoryTree()` for better consistency
- [x] Fix budget setting in `CategoryFormViewModel.java`
- [x] Verification
    - [x] Add unit test for exact minimum budget
    - [x] Add unit test for parent budget propagation
    - [x] Run all unit tests
    - [x] Manual verification on device
