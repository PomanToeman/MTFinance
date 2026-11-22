# Audit Protocol

Guidelines for inspecting and analyzing codebases and technical behavior before planning or implementation.

## Guidelines
1. **Technical Investigation**: Use code search, grep, and file inspection to analyze existing code behavior and architectural patterns.
2. **Systematic Code Quality Inspections**: Use the IDE AST inspector tool (`analyze_file`) across codebase files to systematically detect and eliminate unused imports, dead code, type mismatches, and style warnings.
3. **No Prompt Substitution**: Code audits MUST NOT be used as a substitute for clarifying ambiguous user prompt requirements. Use `ask_user` or prompt questions for prompt ambiguities.
4. **Evidence-Based Findings**: Document findings with direct file paths and line ranges.
