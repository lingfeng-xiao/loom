# Server-Driven Workflow

## Source of truth

- Writable code lives on the server in `/home/lingfeng/loom`.
- The server main worktree must be a real git checkout on branch `main` with `origin` attached.
- Local business code is a read-only mirror, not the place to make primary edits.
- Local Codex reads `.mirror/server-head/` for search, planning, and review.

## Daily flow

1. Run `sync-server-mirror`.
2. Verify remote Claude with `ensure-remote-claude-ready`; sync local user config only if verification fails or config drifted.
3. Create a brief with `new-delegation`.
4. Delegate the task to the server with `delegate-to-claude` or `delegate-to-omc-team`.
5. Pull remote artifacts, including `preflight.json`, `result.json`, and `review-result.json`.
6. Let Codex review against the brief and automatically issue minimal fix passes when the review fails.
7. Write `closeout.json` only after `REVIEW_RESULT: PASS`.
8. Run the server release flow when the task is ready.

## Remote execution roots

- Main repo: `/home/lingfeng/loom`
- Task worktrees: `/home/lingfeng/worktrees`
- Delegation artifacts: `/home/lingfeng/loom/.delegations`
- Release reports: `/home/lingfeng/loom/.release`
- Runtime state kept outside git: `/home/lingfeng/loom/.env`, `/home/lingfeng/loom/logs`, `/home/lingfeng/loom/vault`, `/home/lingfeng/loom/.tmp`

## Release flow

- `deploy/scripts/server-validate.sh`
- `deploy/scripts/server-deploy.sh`
- `deploy/scripts/server-healthcheck.sh`
- `deploy/scripts/server-release-report.sh`
- `deploy/scripts/server-release.sh`
- `deploy/scripts/server-rollback.sh`

Each release writes a folder under `.release/<release-id>/` with validation, deploy, healthcheck, and rollback data.

## Guardrails

- `sync-server-mirror` fails closed when `/home/lingfeng/loom` does not have a valid git `HEAD`.
- Mirror refresh prefers `rsync` when both ends provide it and falls back to a tar stream otherwise.
- Remote `claude -p` is required for single-task delegation and is verified in `preflight.json`.
- Local wrappers verify remote `claude -p` before delegation and sync local user config only when the remote environment is not ready.
- Parallel delegation uses isolated remote `claude -p` workers and does not require `omc team` to be healthy.
- Single-task runs must not report `SUCCESS` unless the worker contract is complete, validation is reported, and a real diff exists.
- Single-task wrappers will auto-loop minimal fix passes and stop only on `PASS`, retry exhaustion, or explicit ambiguity.
- Server-side Claude runs carry both an overall timeout and an idle-output timeout so hung runs fail closed.
- Team runs fail closed if they do not produce per-subtask result files and diffs.
- The deploy path can fall back to direct `docker compose` when `loom.service` is visible but passwordless `sudo` is not available.
