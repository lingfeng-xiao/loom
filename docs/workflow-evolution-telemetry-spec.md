# Workflow Evolution Telemetry Spec

Effective date: 2026-04-11

## Purpose

Monitor the full Codex-led workflow from planning through closeout. The first implementation stores telemetry as repo-local JSON and JSONL artifacts. External dashboards can be added later after the local event model is stable.

## Event schema

Each event is written to `.delegations/<task-id>/telemetry/events.jsonl`.

Required fields:

- `event_id`
- `workflow_id`
- `task_id`
- `attempt`
- `phase`
- `from_state`
- `to_state`
- `status`
- `actor`
- `started_at`
- `finished_at`
- `duration_ms`
- `worktree`
- `branch`
- `files_scope`
- `review_result`
- `validation_result`
- `release_id`
- `rollback_ref`
- `error_code`
- `error_summary`

Core event types:

- `workflow.planned`
- `workflow.split`
- `delegation.dispatched`
- `delegation.running`
- `delegation.completed`
- `delegation.timed_out`
- `delegation.idle_timed_out`
- `review.passed`
- `review.rejected`
- `retry.dispatched`
- `validation.passed`
- `validation.failed`
- `release.started`
- `release.succeeded`
- `release.failed`
- `rollback.ready`
- `rollback.executed`
- `workflow.closed`
- `workflow.abandoned`

## Daily rollup schema

Daily rollups are written to `.workflow-observability/daily/YYYY-MM-DD.json`.

Required metrics:

- `tasks_total`
- `tasks_closed`
- `tasks_abandoned`
- `first_pass_review_rate`
- `fix_loop_total`
- `retry_exhausted_total`
- `timeout_total`
- `idle_timeout_total`
- `review_reject_total`
- `validation_fail_total`
- `deploy_total`
- `deploy_fail_total`
- `rollback_total`
- `phase_duration_ms_p50`
- `phase_duration_ms_p90`
- `end_to_end_duration_ms_p50`
- `end_to_end_duration_ms_p90`
- `top_failure_reasons`
- `top_slowest_phases`

## Reporting cadence

- Daily: generate one JSON rollup for the prior day.
- Weekly: generate `.workflow-observability/weekly/YYYY-Www.md` with trends and recommended adjustments.
- Monthly: review retry budgets, timeout thresholds, file-overlap rules, and release gates.

Optimization questions:

- Are tasks timing out because they are too broad?
- Which review reasons repeat?
- Which validation checks fail most often?
- Does parallel execution reduce lead time without increasing review rejects?
- Should timeout or idle-timeout thresholds change?
