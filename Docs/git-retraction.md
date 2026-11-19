# Git Retraction Protocol

Decision matrix for handling git retractions, rollbacks, and recovery operations.

## Decision Matrix

| Retraction Severity | Situation / Fault Description | Recommended Command | Scope |
| :--- | :--- | :--- | :--- |
| **Local Retraction** | Minor to medium uncommitted edits in working tree that introduced a fault or unnoticeable regression. | `git restore <file>` or `git checkout <file>` | Discards unsaved file modifications in working tree. |
| **Local Retraction** | Last commit message or small single-file commit needs immediate minor correction before pushing. | `git commit --amend` | Replaces the HEAD commit with updated staged changes/message. |
| **Critical Retraction** | Deeply-rooted fault spanning multiple commits or layers that breaks the build or architectural invariants. | `git reset --hard HEAD~1` or `git reset --soft HEAD~N` | Resets branch pointer back to a known clean state. |
| **Critical Retraction** | Fault discovered in a published/pushed commit that cannot be safely reset. | `git revert <commit-hash>` | Creates an inverted commit that safely undoes the faulty change. |
