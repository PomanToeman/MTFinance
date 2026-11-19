# Git Commit Protocol

Guidelines and rules for git operations, commit message formatting, and commit staging.

## Commit Rules
1. **State Message Before Execution**: Explicitly announce the exact commit message before executing `git commit`.
2. **Atomic Commits**: Group related changes into single, focused commits by architecture layer or component.
3. **Pre-Commit Verification**: Run `:app:assembleDebug` and `:app:testDebugUnitTest` to ensure all tests pass before committing.

## Commit Message Format
All commits MUST follow conventional commit syntax:
`<type>(<scope>): <optional Header> <description>`

### Types:
- `feat`: New feature or capability.
- `fix`: Bug fix.
- `refactor`: Code refactoring without behavior change.
- `test`: Adding or refactoring unit/integration tests.
- `docs`: Documentation updates.
- `style`: Formatting, whitespace, or style changes.
