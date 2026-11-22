# Regression Prevention Protocol

Guidelines for preventing regressions and unintended code/test deletions.

## Guidelines
1. **Git Diff & UI Layout Auditing**: Use `git diff` and `git show` to audit changes for accidental deletions, unintended test oracle modifications, or UI layout dimension/method signature shifts before committing.
2. **Copy-Paste Over Rewriting**: Prefer copying and pasting existing methods when transferring or refactoring code across files rather than rewriting from scratch (which introduces subtle syntax/logic regressions).
3. **Test Oracle Protection**: Test assertions and test oracles MUST remain 100% intact during code/test refactoring.
