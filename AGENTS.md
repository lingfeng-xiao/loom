# AGENTS.md

- Prefer `delegate-to-omc` for bounded execution, repeatable work, and parallelizable subtasks.
- Default flow: plan -> mirror sync -> delegate -> review -> validate -> deploy -> close.
- The server workspace `/home/lingfeng/loom` is authoritative; local code is a read-only mirror only.
- Do not run multiple agents in the same worktree at the same time.
- Remote task worktrees must stay under `/home/lingfeng/worktrees`.
- Do not skip validation, release reporting, or review.
