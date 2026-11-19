# Verification Protocol

Mandatory pre-commit verification steps to guarantee code quality and architectural integrity.

## Verification Checklist
1. **App Compilation**: Execute `./gradlew :app:assembleDebug` to confirm zero build/syntax errors.
2. **Test Suite Verification**: Execute `./gradlew :app:testDebugUnitTest` (unit tests) and `./gradlew :app:connectedDebugAndroidTest` (instrumented tests) to verify all unit and instrumented tests pass before committing.
3. **App Launch Check**: Ensure the application compiles and launches without immediate crashes.
4. **Architectural Conformity**:
   - **MVVM Dumb UI**: Compose screens must remain pure rendering surfaces with zero business logic.
   - **CQRS Isolation**: Service classes handle read queries only; write operations pass through Repositories.
   - **Encapsulation**: State fields and internal helper methods must remain private/encapsulated.
