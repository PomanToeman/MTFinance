# Testing Protocol

Standards for writing, organizing, and executing unit and integration tests in MTFinance.

## Testing Standards
1. **SOLID Principles**: Each test method checks exactly one logical behavior (Single Responsibility).
2. **Naming Convention**: Test names MUST follow `method_conditions_expectedResult()`.
3. **No Magic Numbers / Literals**: Raw numeric literals, dates, and string IDs MUST be extracted into named `private static final` constants.
4. **Strong Assertions**: Use exact equality assertions (`assertEquals`) instead of vague range checks (`compareTo >= 0`), unless lower-bound threshold checks are explicitly required by domain rules.
5. **Exception Handling**: Use JUnit 5 `assertThrows(ExceptionClass.class, () -> ...)` for exception assertions.
6. **Grouped Assertions**: Use `assertAll(...)` for multi-property verification on a single test outcome.
