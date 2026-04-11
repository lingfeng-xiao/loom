# Workflow Evolution Plan

Effective date: 2026-04-11

## Goal

Build a durable workflow for Loom where Codex is the control plane and server-side Claude workers are bounded executors. The workflow must cover the full path from user request to plan, requirement split, remote execution, strict Codex review, automatic fix pass, validation, deploy, rollback evidence, closeout, and monitoring.

This wave is about the development and delivery workflow itself. It is not a product-surface change.

## Current pain points

- Local wrapper logic is split across PowerShell and Bash even though execution is server-first.
- Task state is mostly inferred from files such as `result.json`, `review-notes.md`, and release logs instead of a formal state model.
- Parallel execution is isolated by worktree, but worktree locks, file-surface locks, and release locks are not yet first-class gates.
- Review and retry behavior exists, but it is not fully modeled as durable workflow state.
- Release evidence exists under `.release/`, but delegation closeout does not yet have a strong release and rollback linkage.
- Workflow telemetry is not aggregated across planning, dispatch, review, validation, deploy, and closeout.

## Target workflow architecture

Codex is the control plane:

- turn user goals into a plan
- split the plan into bounded requirements
- assign each requirement a task id, file surface, validation rule, and acceptance rule
- dispatch remote Claude workers to isolated server worktrees
- review worker output strictly
- issue minimal fix passes until the review passes or the retry budget is exhausted
- validate, deploy, and close only when gates pass
- record telemetry for every phase

Server-side workers are the execution plane:

- execute only inside `/home/lingfeng/worktrees/<task-id>`
- do not make architecture decisions outside the brief
- emit a complete worker contract
- leave git status and diff evidence
- never share one worktree between concurrent workers

The dispatch path must be protected by a frozen runner baseline:

- create a stable server worktree, for example `/home/lingfeng/worktrees/workflow-runner-baseline`
- use that baseline only to dispatch the current wave
- develop new workflow code in separate implementation worktrees
- do not switch production dispatch to the new workflow until the rollout gates pass

The future direction is Bash-only server execution. PowerShell wrappers are legacy compatibility, not the target architecture.

## State model

Each workflow task must move through explicit states:

- `planned`: Codex has a plan but no task split exists.
- `split`: Codex has task briefs with file surface, validation, and acceptance.
- `dispatched`: a remote worker has been assigned.
- `running`: the remote worker is executing.
- `timed_out`: the overall timeout killed the worker.
- `idle_timed_out`: the idle-output timeout killed the worker.
- `review_failed`: Codex review rejected the worker result.
- `retry_dispatched`: a minimal fix pass has been sent.
- `validated`: required validation has passed.
- `deploy_ready`: review and validation gates are green.
- `deployed`: server release completed and healthcheck passed.
- `closed`: closeout evidence is complete.
- `abandoned`: retry budget is exhausted or the brief is ambiguous.

Each transition writes an event to task telemetry. Terminal states are `closed` and `abandoned`.

## Monitoring model

The first monitoring phase writes repo-local artifacts before adding external dashboards:

- `.delegations/<task-id>/telemetry/events.jsonl`
- `.delegations/<task-id>/telemetry/summary.json`
- `.workflow-observability/daily/YYYY-MM-DD.json`
- `.workflow-observability/weekly/YYYY-Www.md`

Minimum metrics:

- task count by state
- first-pass review rate
- fix-loop count
- retry exhaustion count
- timeout count
- idle-timeout count
- validation failure count
- deploy success and failure count
- rollback count
- end-to-end duration
- phase duration by planning, dispatch, worker, review, validation, deploy, and closeout

Daily rollups should answer:

- which phase is slowest
- which task type is rejected most often
- which failure reasons dominate
- whether timeout thresholds need adjustment
- whether the workflow should be upgraded, simplified, or made stricter

## Migration strategy

Phase 1: document and seed.

- create this upstream plan
- create the workflow requirements wave
- create telemetry, test, migration, and operator supporting docs
- keep the current dispatch path unchanged

Phase 2: develop behind the frozen runner.

- create implementation worktrees from a seed branch
- keep legacy dispatch available
- add state, lock, telemetry, and release-gate implementation in small tasks
- run all tests in sandbox worktrees

Phase 3: self-test the new workflow.

- use the new workflow only on non-critical sandbox tasks
- compare telemetry with manual evidence
- run timeout, idle-timeout, review-fail, deploy-fail, and rollback injections

Phase 4: controlled cutover.

- switch production dispatch only after the new runner passes regression
- keep the legacy runner available for rollback
- remove legacy wrappers only after stable operation over the agreed observation window

## Acceptance and rollout gates

The workflow is not ready for cutover until all gates pass:

- `requirements gate`: every task has source, dependency, file surface, validation, and acceptance.
- `dispatch gate`: each worker uses a unique server worktree under `/home/lingfeng/worktrees`.
- `lock gate`: worktree, file-surface, and release locks prevent unsafe overlap.
- `review gate`: Codex review passes or the task enters bounded fix-loop state.
- `validation gate`: required validation passes and evidence is attached.
- `release gate`: deploy only runs from `deploy_ready`.
- `rollback gate`: release evidence includes rollback target and rollback readiness.
- `telemetry gate`: task summary and daily rollup are written.
- `cutover gate`: the new workflow passes sandbox self-test and legacy fallback remains available.
