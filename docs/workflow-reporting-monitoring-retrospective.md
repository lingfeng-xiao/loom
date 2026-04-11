# Workflow Reporting And Monitoring Retrospective

Effective date: 2026-04-11

## Summary

This report records the issues encountered while implementing workflow reporting
and issue monitoring. It is intentionally kept as a tracked example of the
closeout transparency expected from future workflow runs.

## Issues observed

- `claude_invocation_error`: Phase 1 workers initially failed with `Execution error` because the prompt was passed after CLI flags. The verified form is `claude --print "$prompt" --output-format text ... </dev/null`.
- `shell_quote_error`: Inline remote shell commands were repeatedly misread by the remote default zsh when values contained spaces or quoted grep patterns.
- `runner_error`: PowerShell-to-remote heredoc style failed; uploading LF-normalized temporary Bash scripts was more reliable.
- `artifact_sync_error`: An early `scp` target path was too broad in a previous run and had to be corrected before commit.
- `mirror_head_mismatch`: `.mirror/server-head` has no `.git`, so mirror HEAD must be read from `.mirror-meta.json`.
- `review_rejected`: Several Claude worker attempts produced useful diffs but did not return the required result contract, so Codex had to review and minimally repair the changes.
- `validation_failed`: The first QA test pass found two real implementation bugs: report helper execution depended on executable bit, and daily rollup mishandled absolute observability paths.

## Recovery actions

- Kept `/home/lingfeng/worktrees/workflow-runner-baseline` frozen and did not switch dispatch to the code being modified.
- Accepted only reviewed diffs, rejected incomplete worker contracts, and used minimal Codex repair where Claude repeatedly returned no diff.
- Added mandatory `workflow-report.md` and `workflow-report.json` so future runs disclose issues without requiring the user to ask.
- Added issue telemetry and rollup helpers so these problems can be counted by category and severity.

## Residual risk

The direct Claude CLI still occasionally returns `Execution error` after making
partial edits. Future workflow runs should treat that as an issue event and
continue to rely on Codex review, real diff checks, and bounded retry/fix loops.
