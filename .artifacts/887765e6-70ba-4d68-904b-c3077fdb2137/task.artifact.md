# Task List - Update Tests for Category Dashboard and Repository Refinements

- [ ] Update `TrackingRepositoryTest.java`
    - [ ] Update `testFindTotalInCategoryWithDateRangeAndSubs` for new signature
    - [ ] Add `testGetTransactionsForCategory` test case
    - [ ] Add `testGetChildrenTotals` test case
- [ ] Update `CategoryViewModelTest.java`
    - [ ] Update `setUp` to observe new LiveData fields
    - [ ] Add `setSelectedCategory_loadsDashboardData` test case
    - [ ] Add `resetSelectedCategory_clearsDashboardData` test case
- [ ] Verification
    - [ ] Run all unit tests (`testDebugUnitTest`)
    - [ ] Run instrumented tests (`connectedDebugAndroidTest`)
