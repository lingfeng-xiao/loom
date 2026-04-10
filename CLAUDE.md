# CLAUDE.md

- Plan before coding.
- Clarify scope and constraints before editing.
- Prefer the smallest safe diff.
- The server at `/home/lingfeng/loom` is the only writable code source of truth.
- Local business code edits are disallowed; use the read-only mirror at `.mirror/server-head/` for search and review.
- Execution-heavy tasks should go through `delegate-to-omc` and run on the server.
- Parallel work must use isolated git worktrees.
- Remote task worktrees live under `/home/lingfeng/worktrees`.
- Run validation before calling work complete.
- Failed validation is not a finished task.
- Deploy through the server release scripts, not GitHub Actions.
