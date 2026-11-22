# Audit Protocol

Guidelines for inspecting and analyzing codebases and technical behavior before planning or implementation.

## Guidelines
1. **Technical Investigation**: Use code search, grep, and file inspection to analyze existing code behavior and architectural patterns.
2. **Systematic Code Quality Inspections**: Use the IDE AST inspector tool (`analyze_file`) across codebase files to systematically detect and eliminate unused imports, dead code, type mismatches, and style warnings.
3. **String Literal Encapsulation**: Every single non-trivial string literal (user messages, validation warnings, exception formats, log templates) MUST be encapsulated in a `MessageCli` implementation inside the `com.example.mtfinance.src.messages` package (e.g. `ErrorMessages`, `TransactionMessages`, `GeneralMessages`, `CategoryMessages`, `ImportMessages`). Trivial literals like `""`, `0`, or `-1` are permitted inline.
4. **No Prompt Substitution**: Code audits MUST NOT be used as a substitute for clarifying ambiguous user prompt requirements. Use `ask_user` or prompt questions for prompt ambiguities.
5. **Evidence-Based Findings**: Document findings with direct file paths and line ranges.
