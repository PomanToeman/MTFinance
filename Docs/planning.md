# Planning Protocol

Standards for creating, refining, and executing technical implementation plans.

## Plan Requirements
1. **Goal & Background**: Clear description of the objective and technical context.
2. **File Change Demarcation**: Explicitly classify every changed file with tags:
   - `[NEW]` for newly created files.
   - `[MODIFY]` for existing modified files.
   - `[REFACTOR]` for structure/refactoring changes without logic alteration.
   - `[DELETE]` for removed files.
3. **Assumptions & Open Questions**: Disclose technical assumptions and state up to 3 open questions for prompt ambiguities.
4. **Verification Plan**: Detail exact build commands and test runs required before completion.
