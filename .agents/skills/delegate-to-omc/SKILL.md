---
name: delegate-to-omc
description: Delegate bounded execution work from local Codex to remote Claude Code or OMC running on the server. Use when Codex should stay in planner/reviewer/closer mode while a server-side worker handles a clear implementation task, low-risk refactor, batch edit, test backfill, docs pass, or parallel subtask set inside isolated remote git worktrees.
---

# Delegate To OMC

## Purpose

Use this skill to hand execution work from local Codex to server-side `claude -p` workers without manual copy-paste.

Keep Codex in the `planner / reviewer / closer` role:

- decide whether the task is safe to delegate
- run the environment preflight and verify remote Claude first, syncing user config only when needed
- generate a delegation packet
- call the local wrapper that reaches the server through `ssh jd`
- review the result against the brief
- keep issuing minimal fix passes until the review passes or the retry budget is exhausted
- close the task only after the review passes

## When To Use

Good fits:

- bounded feature work
- local bug fixes
- low-risk refactors
- batch edits
- test backfills
- docs completion
- cleanly parallel subtasks

Avoid delegation when the task is still unclear or the risk is high:

- core architecture rewrites
- risky database migrations
- production operations
- security / auth / billing critical rewrites
- ambiguous product requests

## Decision Policy

Choose one path before creating the packet:

- Use remote `claude -p` for one bounded task with a clear file surface, low coupling, and low risk.
- Use parallel remote `claude -p` workers for several low-coupling subtasks that can run in isolated server worktrees without touching the same files.
- Generate the brief only when tools are missing, the runtime is unstable, or the task is specified enough to package but not safe to auto-run.
- Keep the work in Codex when the task needs strong architecture judgment, heavy uncertainty handling, or high-risk decision making.

Default bias:

- single task: `claude -p`
- parallel subtasks: isolated parallel `claude -p` workers
- uncertain or high-risk task: Codex handles it directly

## Delegation Packet Contract

Every delegated task must have a brief with these fields:

- `Task ID`
- `Goal`
- `Context`
- `Constraints`
- `Done when`
- `Non-goals`
- `Relevant files`
- `Validation`
- `Risks to watch`
- `Expected output format`

Use the template in [assets/task-brief-template.md](assets/task-brief-template.md). The brief is created locally, uploaded to the server, and treated as the source of truth for the remote worker.

Before any live run, verify remote `claude -p` first. Sync the local Claude user config to the server only if verification fails, local config changed, or you explicitly want to refresh the remote Minmax / user settings.

## Output Contract

Require the worker response to include:

- `RESULT: SUCCESS | PARTIAL | FAILED`
- `SUMMARY:`
- `CHANGED_FILES:`
- `TESTS_RUN:`
- `RISKS:`
- `BLOCKERS:`
- `NEXT_ACTIONS:`

If any section is missing, treat the run as incomplete and review it as `NEEDS_FIX`.

The remote scripts now also write:

- `preflight.json` with remote environment checks
- `result.json` with contract and diff verdicts
- `review-result.json` with Codex's machine-readable review verdict and fix list
- `closeout.json` after Codex marks the review as `PASS`

## Codex Review Flow

After the remote worker finishes:

1. Read `brief.md`.
2. Check the result against `Done when` and `Non-goals`.
3. Inspect the diff and reject out-of-scope edits.
4. Confirm validation commands were actually run, or that the worker explicitly explained why they were not.
5. Write `PASS` or `NEEDS_FIX` in `review-notes.md`.
6. When the review fails, append the minimal fix list to a retry brief and re-dispatch the same task.
7. Stop only after the review passes, the retry budget is exhausted, or you hit a real ambiguity that needs user input.
8. Write `closeout.json` only after `PASS`.

Use [assets/review-checklist.md](assets/review-checklist.md) as the fixed checklist.

## Runtime Guardrails

- Default to `ensure-remote-claude-ready` before live delegation. It verifies `claude -p` first and syncs local user config only when the remote environment drifted.
- Pass `TimeoutSeconds` and `IdleTimeoutSeconds` through the wrappers so server-side Claude runs fail closed instead of spinning forever.
- Treat timeout or idle timeout as hard review failures and feed them back into the minimal fix loop.

## Worktree Rule

Parallel work must be isolated on the server:

- every parallel subtask gets its own remote git worktree and its own `claude -p` run
- do not run multiple workers in the same worktree against the same file set
- prefer `/home/lingfeng/worktrees/<task-id>` so the isolation is visible and stable

Use the local scripts in `scripts/` so the delegation packet, remote worktree path, and result artifacts stay consistent. Refresh `.mirror/server-head/` before planning or reviewing.
