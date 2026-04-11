# Workflow Evolution Operator Guide

Effective date: 2026-04-11

## Purpose

This guide describes how Codex should operate the upgraded workflow. Codex owns planning, scheduling, review, validation, deploy, closeout, and monitoring review. Server Claude workers own only bounded execution.

## Daily operating flow

1. Sync or inspect the server source of truth.
2. Turn the user request into a plan.
3. Split the plan into bounded tasks with source, dependency, file surface, validation, and acceptance.
4. Check worktree and file-surface conflicts before dispatch.
5. Dispatch remote Claude workers to isolated worktrees.
6. Review each result strictly against the brief.
7. Send minimal fix passes until review passes or retry budget is exhausted.
8. Run required validation.
9. Deploy only after review and validation pass.
10. Write closeout with release id and rollback ref.
11. Generate `workflow-report.md` and `workflow-report.json`; do not call the work closed without them.
12. Summarize problems, expectation mismatches, evidence paths, and residual risk in the user-facing final response.
13. Review daily workflow telemetry before changing thresholds or gates.

## Incident handling

- Timeout: kill the worker, mark `timed_out`, capture partial diff, and retry only with a smaller brief.
- Idle timeout: kill the worker, mark `idle_timed_out`, and tighten the task prompt before retry.
- Review rejection: write the issue list, generate a minimal fix brief, and re-dispatch within the retry budget.
- Retry exhaustion: mark `abandoned`, do not deploy, and surface the ambiguity or repeated failure.
- Validation failure: block deploy and create a fix task.
- Deploy failure: require rollback decision and write release failure evidence.
- Rollback failure: stop further deploy attempts and preserve all release artifacts for manual diagnosis.
- Shell quote or runner error: record `workflow.issue.detected`, preserve command preview and runner output, and prefer uploaded Bash scripts over inline shell.
- Release-ahead warning: record `release_ahead_warning` and do not push remote history without a separate push audit.

## Optimization loop

- Daily: inspect timeout rate, review reject rate, validation failure rate, deploy failure rate, and end-to-end duration.
- Weekly: identify recurring rejection reasons, slow phases, and unstable task types.
- Monthly: adjust retry budgets, timeout thresholds, file-overlap checks, and release gates.
- Do not loosen review gates just to improve success rate; use telemetry to improve task splitting and prompts first.
