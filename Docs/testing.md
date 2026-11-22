# Testing Protocol

Standards for writing, organizing, and executing unit and integration tests in MTFinance.

## Testing Standards
1. **SOLID Principles**: Each test method checks exactly one logical behavior (Single Responsibility).
2. **Naming Convention**: Test names MUST follow `method_conditions_expectedResult()`.
3. **No Magic Numbers / Literals**: Raw numeric literals, dates, and string IDs MUST be extracted into named `private static final` constants.
4. **Testing Framework Scope**:
   - **JUnit 5 (Unit Tests in `src/test`)**: Use JUnit 5 assertions, `@ParameterizedTest`, `assertThrows(ExceptionClass.class, () -> ...)`, and `assertAll(...)` for multi-property verification.
   - **JUnit 4 (Android Instrumented Tests in `src/androidTest`)**: Use JUnit 4 assertions (`org.junit.Assert.*`) and `@Test(expected = ExceptionClass.class)` due to AndroidX Test Runner compatibility constraints.
5. **Strong Assertions**: Use exact equality assertions (`assertEquals`) instead of vague range checks (`compareTo >= 0`), unless lower-bound threshold checks are explicitly required by domain rules.
