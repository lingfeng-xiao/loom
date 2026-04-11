# Workflow Evolution Test Plan

Effective date: 2026-04-11

## Goal

Validate the workflow evolution from Codex planning to server-side execution, review, retry, validation, deploy, rollback, telemetry, and closeout.

## Test matrix

| Test ID | Scenario | Expected result |
| --- | --- | --- |
| WF-PLAN-01 | Codex creates an upstream plan and task split | Plan and task briefs have source, dependency, validation, and acceptance |
| WF-DISP-01 | Single task dispatches to one remote worktree | Worker runs under `/home/lingfeng/worktrees/<task-id>` |
| WF-DISP-02 | Parallel tasks dispatch to distinct worktrees | No shared worktree and no file overlap |
| WF-LOCK-01 | Two tasks target the same worktree | Second task is blocked |
| WF-LOCK-02 | Two tasks overlap on the same file surface | Dispatch is blocked until the first task closes |
| WF-LOCK-03 | Two releases start at the same time | Release lock blocks the second release |
| WF-REVIEW-01 | Worker result is incomplete | Review rejects and a fix pass is issued |
| WF-REVIEW-02 | Fix-loop exhausts retry budget | Task enters `abandoned` and no closeout PASS is written |
| WF-TIME-01 | Overall timeout fires | Worker is killed, `timed_out` is recorded, no orphan process remains |
| WF-TIME-02 | Idle-output timeout fires | Worker is killed, `idle_timed_out` is recorded |
| WF-VAL-01 | Validation fails | Deploy is blocked |
| WF-DEPLOY-01 | Release succeeds | Release id and rollback ref are linked to closeout |
| WF-DEPLOY-02 | Healthcheck fails | Release enters failure state and rollback decision is required |
| WF-OBS-01 | Task emits events | `events.jsonl` and `summary.json` are written |
| WF-OBS-02 | Daily rollup runs | Daily JSON includes task, retry, timeout, review, validation, and deploy metrics |

## Failure injection

- Force timeout with a deliberately small total timeout.
- Force idle timeout with a worker that produces no output.
- Force review rejection with missing contract fields.
- Force validation failure with a known failing command.
- Force release contention by holding the release lock.
- Force file overlap by launching two tasks against the same target file.
- Force rollback decision by making healthcheck fail after deploy.

## Exit criteria

- All dispatch and lock tests pass.
- No timeout test leaves an orphan Claude process.
- Review failure never writes a successful closeout.
- Validation failure never reaches deploy.
- Release failure always produces rollback evidence.
- Daily rollup includes timeout, idle-timeout, review-reject, validation-fail, deploy-fail, and rollback counts.
- Legacy runner remains available until the new runner passes sandbox self-test and the observation window.
