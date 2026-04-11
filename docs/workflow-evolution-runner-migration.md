# Workflow Runner Migration

Effective date: 2026-04-11

## Purpose

Move the workflow toward Codex directly orchestrating server-side Bash scripts while avoiding disruption during the migration. PowerShell wrappers and mixed local wrappers become legacy compatibility paths, not the target execution model.

## Current baseline

The current workflow already executes real work on the server, but some local wrappers still handle upload, dispatch, and artifact pullback. This adds platform-specific behavior and makes the dispatch path harder to reason about.

For this wave, freeze a stable runner baseline:

- baseline path: `/home/lingfeng/worktrees/workflow-runner-baseline`
- baseline role: dispatch only
- implementation worktrees: separate from the baseline
- production dispatch: unchanged during implementation

## Target runner model

The target runner model is Bash-only on the server:

- Codex prepares the plan and task briefs.
- Codex uses `ssh/scp` to send briefs and call server Bash scripts.
- Server Bash scripts own preflight, worktree creation, timeout handling, telemetry, validation, release, and closeout evidence.
- Remote Claude workers only execute bounded work in assigned worktrees.
- Local PowerShell is removed after the new server runner is proven stable.

## Cutover phases

1. Seed the wave with docs and requirements.
2. Build the new runner behind the frozen baseline.
3. Run sandbox self-tests for single-task, parallel-task, timeout, idle-timeout, review-fail, deploy-fail, and rollback scenarios.
4. Switch a non-critical dispatch path to the new runner.
5. Observe telemetry for the agreed window.
6. Move production dispatch to the new runner.
7. Remove legacy PowerShell only after stable operation and rollback proof.

Rollback is always to the frozen baseline runner until the legacy path is intentionally retired.
