# Delegate To OMC Test Report

## Environment baseline

- Test branch: `codex/delegate-to-omc-stabilization`
- Local execution root: `C:\Users\16343\Desktop\loom\.worktrees\codex-delegate-to-omc-stabilization`
- Remote validation repo: `/home/lingfeng/worktrees/codex-delegate-to-omc-stabilization-repo`
- Remote delegation root for validation: `/home/lingfeng/worktrees/codex-delegate-to-omc-stabilization-repo/.delegations`
- Local Claude user config was synced to `jd:~/.claude` with `sync-claude-user-config.ps1`
- Remote `claude -p --setting-sources user,project` returned `CLAUDE_P_OK`
- Remote `tmux` and `omc` were present on the server, but the stable delegation path now uses remote Claude workers directly

## Layer 1: static structure tests

- Added `sync-claude-user-config.ps1/.sh` to make local Minmax and Claude user-level config sync explicit.
- Added `ensure-remote-claude-ready.ps1/.sh` so daily runs verify remote `claude -p` first and sync local config only on drift or explicit refresh.
- Added `review-delegation.ps1` so Codex review writes `review-result.json` and a minimal fix list that can feed the next retry brief.
- Added `server-delegation-preflight.sh` to record `preflight.json` before each live run.
- Added `close-delegation.ps1/.sh` to gate `closeout.json` on `REVIEW_RESULT: PASS`.
- Updated single-task result handling to require:
  - full output contract
  - explicit validation reporting
  - real git diff or git status evidence
- Updated team handling to:
  - allocate isolated worktrees per subtask
  - launch one remote `claude -p` worker per subtask in parallel
  - capture per-subtask `result.json`, `git.status.txt`, and `git.diff.stat.txt`
  - aggregate parent status only after all child workers finish
- PowerShell help and Bash syntax checks passed for the new and modified scripts.
- `sync-claude-user-config.ps1 -VerifyOnly` passed against `jd`.
- `ensure-remote-claude-ready.ps1` passed against `jd` and skipped sync because remote Claude was already healthy.

## Layer 2: dry-run tests

### Single-task dry-run

- Task id: `_stabilize_dry_single`
- Command: `delegate-to-claude.ps1 -DryRun`
- Result: `DRY_RUN`
- Verified artifacts:
  - `brief.md`
  - `preflight.json`
  - `prompt.sent.md`
  - `command.preview.txt`
  - `claude.response.md`
  - `git.status.txt`
  - `git.diff.stat.txt`
  - `result.json`

### Team dry-run

- Task id: `_stabilize_team_dry`
- Command: `delegate-to-omc-team.ps1 -DryRun`
- Result: `DRY_RUN`
- Verified artifacts:
  - `team.prompt.md`
  - `worktrees.json`
  - `preflight.json`
  - `omc.response.md`
  - `git.status.txt`
  - `git.diff.stat.txt`
  - `result.json`

## Layer 3: live tests

### Live Claude single-task positive smoke

- Task id: `_stabilize_live_single`
- Scope: create `delegate-smoke-positive.txt` inside the assigned remote worktree
- Result: `SUCCESS`
- Evidence:
  - `preflight_status=PASS`
  - `contract_complete=true`
  - `diff_present=true`
  - `validation_reported=true`
  - `git.status.txt` shows `A  delegate-smoke-positive.txt`
- Review result: `PASS`
- Closeout result: `close-delegation.ps1` succeeded and wrote `closeout.json`

### Live Claude single-task negative smoke

- Task id: `_stabilize_live_negative`
- Scope: intentionally return a success-style contract without making any real change
- Result: `PARTIAL`
- Evidence:
  - worker declared `RESULT: SUCCESS`
  - `diff_present=false`
  - wrapper downgraded the run to `PARTIAL`
- Review result: `NEEDS_FIX`
- Closeout result: blocked as expected because review did not pass

### Live parallel Claude team smoke

- Task id: `_stabilize_team_parallel_live`
- Scope: two isolated subtasks creating `delegate-team-a.txt` and `delegate-team-b.txt`
- Result: `SUCCESS`
- Evidence:
  - parent `result.json` reports `worker_status=SUCCESS`
  - both subtask `result.json` files report `worker_status=SUCCESS`
  - each subtask has its own isolated worktree and preflight/result artifacts
  - parent `git.status.txt` aggregates both subtask worktrees

### Team invalid-path negative smoke

- Task id: `_stabilize_team_negative`
- Scope: invoke the wrapper with an invalid remote repo root
- Result: remote command failed before live execution
- Evidence:
  - remote shell reported missing repo path
  - artifacts were still pulled back for inspection
- Note: this exercises wrapper-level failure rather than server preflight because the script path itself depends on a valid remote repo root

### Timeout and auto-fix loop live smoke

- Task id: `timeout-auto-fix`
- Remote validation repo: `/home/lingfeng/worktrees/delegate-runtime-check`
- Command: `delegate-to-claude.ps1 -TimeoutSeconds 1 -IdleTimeoutSeconds 1 -MaxFixAttempts 1`
- Result: final local exit code `2` after two attempts
- Evidence:
  - first live attempt timed out with `timed_out=true` and `exit_code=124`
  - `review-delegation.ps1` produced `REVIEW_RESULT: NEEDS_FIX`
  - wrapper generated a retry brief and automatically re-dispatched attempt 2
  - second attempt also failed closed and the wrapper stopped after exhausting the retry budget
  - `attempts/attempt-01` and `attempts/attempt-02` snapshots were written locally for audit

### Team dry-run with timeout metadata

- Task id: `team-timeout-dry`
- Remote validation repo: `/home/lingfeng/worktrees/delegate-runtime-check`
- Command: `delegate-to-omc-team.ps1 -DryRun -TimeoutSeconds 7 -IdleTimeoutSeconds 3`
- Result: `DRY_RUN`
- Evidence:
  - parent `result.json` records `timeout_seconds=7`
  - parent `result.json` records `idle_timeout_seconds=3`
  - wrapper verified remote Claude readiness before delegation and skipped redundant config sync

## Current conclusion

- Single-task delegation is stable for server-first dry-run and live smoke use.
- Single-task delegation now has verify-first config handling, strict local review, automatic minimal fix retries, and server-side timeout / idle timeout protection.
- Parallel delegation is also stable through isolated remote Claude workers.
- The environment sync path for local Minmax / Claude config to the server is working.
- The review and closeout gates are enforced by machine-readable artifacts for both single-task and team paths.
