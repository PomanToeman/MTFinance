# AI Agent Protocols & Instructions

General guidance and mandatory instructions for AI developer agents working in the MTFinance codebase.

## Mandatory Agent Rules
1. **Always State Assumptions**: Disclose all technical and architectural assumptions explicitly before proposing solutions.
2. **Clarify Ambiguities**: Ask up to 3 focused open questions for any prompt ambiguities using the `ask_user` tool or query questions. Never substitute code audits for prompt clarification.
3. **Explicit Protocol Declarations**: Always state which active protocol(s) you are following during every turn.
4. **Explicit Non-Modification Promises**: State explicitly what files and layers are NOT being modified (e.g. for pure refactors, declare that production business logic in `src/main` remains 100% untouched).
5. **Google Java Code Style & JavaDocs**: Conform to Google Java Code Style, providing explicit JavaDocs for all non-trivial classes and methods.
6. **Conventional Commit Format**: Commit messages MUST follow `<type>(<scope>): <optional Header> <description>`.

## Protocol References & Links
- [Audit Protocol](file:///C:/Users/Rebec/StudioProjects/MTFinance/docs/audit.md)
- [Planning Protocol](file:///C:/Users/Rebec/StudioProjects/MTFinance/docs/planning.md)
- [Verification Protocol](file:///C:/Users/Rebec/StudioProjects/MTFinance/docs/verification.md)
- [Testing Protocol](file:///C:/Users/Rebec/StudioProjects/MTFinance/docs/testing.md)
- [Git Commit Protocol](file:///C:/Users/Rebec/StudioProjects/MTFinance/docs/git-commit.md)
- [Git Retraction Protocol](file:///C:/Users/Rebec/StudioProjects/MTFinance/docs/git-retraction.md)
- [Regression Prevention Protocol](file:///C:/Users/Rebec/StudioProjects/MTFinance/docs/regression.md)
