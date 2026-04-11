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
- `workflow.issue.detected`
- `session.summary.generated`
- `reflection.candidate_lesson`

Issue events use these categories:

- `runner_error`
- `claude_invocation_error`
- `shell_quote_error`
- `artifact_sync_error`
- `mirror_head_mismatch`
- `preflight_failure`
- `timeout`
- `idle_timeout`
- `review_rejected`
- `validation_failed`
- `deploy_failed`
- `release_ahead_warning`

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
- `issue_total_by_category`
- `issue_total_by_severity`
- `auto_recovered_total`
- `follow_up_required_total`
- `top_issue_summaries`
- `delegated_task_count`
- `worker_success_count`
- `worker_failure_count`
- `contract_failure_count`
- `codex_takeover_count`
- `fix_pass_count`
- `estimated_claude_prompt_tokens`
- `estimated_claude_response_tokens`
- `estimated_codex_overhead_tokens`
- `token_savings_verdict`

## Session summary schema

Session summaries are written to `.delegations/_sessions/<session-id>/delegation-session-summary.json` after all tasks in the session finish. The summary is also printed so Codex can include it directly in the final user response.

Required fields:

- `session_id`
- `delegated_task_count`
- `worker_success_count`
- `worker_failure_count`
- `contract_failure_count`
- `codex_takeover_count`
- `fix_pass_count`
- `review_reject_count`
- `elapsed_time_by_phase`
- `estimated_claude_prompt_tokens`
- `estimated_claude_response_tokens`
- `estimated_codex_overhead_tokens`
- `token_savings_verdict`
- `confidence`
- `events_by_type`
- `statuses`

Token values should use real upstream usage if available. When real usage is not available, estimate Claude prompt and response tokens with `chars / 4`, estimate Codex overhead with stable proxy counters, and mark confidence accordingly.

## V2 session artifacts

V2 sessions use these additional artifacts:

- `session-manifest.json`: desired task DAG/session input
- `session-state.json`: checkpoint and resume state
- `session-events.jsonl`: deterministic event stream
- `gate-summary.json`: compact machine gate result for Codex review
- `user-report.md`: report draft intended to be summarized directly to the user

The runner should write checkpoints before and after every irreversible action.
Machine gates should reduce Codex log-reading by summarizing contract, diff,
scope, validation, preflight, timeout, and repairability.

## Reflection model

Reflection is a post-run path, not a worker execution path. During execution the workflow records facts and evidence only. After all tasks complete, Codex should summarize:

- quality gate result by task
- failed gate, symptom, evidence, counter-evidence, confidence, and repairability
- whether repair size selection was appropriate
- whether Claude/Minmax reduced Codex effort
- which issues are script-fixable, prompt-fixable, process-fixable, or model-boundary problems
- `candidate_lesson` items with confidence and `review_after`

Candidate lessons are not permanent rules until repeated evidence or explicit adoption promotes them to `proposed_rule_change`.

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
