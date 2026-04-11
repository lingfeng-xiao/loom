# Workflow Evolution Operator Guide

Effective date: 2026-04-11

## Purpose

This guide describes how Codex should operate the upgraded workflow. Codex owns planning, scheduling, review, validation, deploy, closeout, and monitoring review. Server Claude workers own only bounded execution.

## Daily operating flow

1. Sync or inspect the server source of truth.
2. Turn the user request into a plan.
3. Split the plan into bounded tasks with source, dependency, file surface, validation, and acceptance.
4. For multi-task sessions, write a `session-manifest.json` and run the v2 admission gate before any Claude dispatch.
5. Check worktree and file-surface conflicts before dispatch.
6. Dispatch remote Claude workers to isolated worktrees.
7. Let the machine gate summarize contract, diff, scope, validation, preflight, timeout, and retry decision before Codex reads full worker logs.
8. Review each result strictly against the brief and machine gate summary.
9. When a review gate fails, triage the failed gate, likely cause, repair size, recommended action, confidence, and evidence before retrying.
10. Re-dispatch to Claude only when the triage action is safely bounded; otherwise resplit, clarify, fix infrastructure, let Codex take over, or block release.
11. Run required validation.
12. Deploy only after review and validation pass.
13. Write closeout with release id and rollback ref.
14. Generate `workflow-report.md`/`workflow-report.json` for single tasks or `user-report.md` for v2 sessions; do not call the work closed without a user-visible report.
15. After all delegated tasks complete, generate the delegation session summary and post-run reflection.
16. Review daily workflow telemetry before changing thresholds or gates.

## Incident handling

- Timeout: kill the worker, mark `timed_out`, capture partial diff, and retry only with a smaller brief.
- Idle timeout: kill the worker, mark `idle_timed_out`, and tighten the task prompt before retry.
- Review rejection: write the issue list and failure triage. Do not default to a minimal fix; choose tiny, small, medium, large, infrastructure fix, resplit, clarify, Codex takeover, or block release based on cause and repairability.
- Retry exhaustion: mark `abandoned`, do not deploy, and surface the ambiguity or repeated failure.
- Validation failure: block deploy and create a fix task.
- Deploy failure: require rollback decision and write release failure evidence.
- Rollback failure: stop further deploy attempts and preserve all release artifacts for manual diagnosis.
- Shell quote or runner error: record `workflow.issue.detected`, preserve command preview and runner output, and prefer uploaded Bash scripts over inline shell.
- Release-ahead warning: record `release_ahead_warning` and do not push remote history without a separate push audit.

## Optimization loop

- During execution: record lightweight dispatch, result, review, validation, fix-pass, takeover, release, and issue evidence.
- After execution: summarize evidence into the user-facing report and session reflection.
- Prefer machine gate summaries over full worker logs; Codex should deep-read logs only for gate failures, high-risk changes, or release decisions.
- Candidate lessons are hypotheses with confidence and a review date; promote them to rule changes only after repeated evidence or explicit adoption.
- Token savings is a goal metric, not an excuse to lower quality gates. Prefer real usage when available; otherwise estimate worker tokens with `chars / 4` and Codex overhead with proxy counts.
- Daily: inspect timeout rate, review reject rate, validation failure rate, deploy failure rate, and end-to-end duration.
- Weekly: identify recurring rejection reasons, slow phases, and unstable task types.
- Monthly: adjust retry budgets, timeout thresholds, file-overlap checks, and release gates.
- Do not loosen review gates just to improve success rate; use telemetry to improve task splitting and prompts first.
