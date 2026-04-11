#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd -P)"
scripts_dir="$repo_root/.agents/skills/delegate-to-omc/scripts"
tmp_root="$(mktemp -d)"
trap 'rm -rf "$tmp_root"' EXIT

delegation_root="$tmp_root/delegations"
session_id="session-roi"
mkdir -p "$delegation_root/task-pass" "$delegation_root/task-fail"

cat > "$delegation_root/task-pass/prompt.md" <<'EOF'
Implement a small bounded change and report the contract.
EOF
cat > "$delegation_root/task-pass/response.md" <<'EOF'
RESULT: SUCCESS
SUMMARY: Done.
CHANGED_FILES: one file
TESTS_RUN: bash test
RISKS: none
BLOCKERS: none
NEXT_ACTIONS: none
EOF
cat > "$delegation_root/task-fail/prompt.md" <<'EOF'
Implement an over-broad task that should be split.
EOF
cat > "$delegation_root/task-fail/response.md" <<'EOF'
RESULT: FAILED
SUMMARY: Contract failed and Codex takeover was required.
EOF

bash "$scripts_dir/record-delegation-event.sh" \
  --session-id "$session_id" \
  --delegation-root "$delegation_root" \
  --event-type delegation.dispatched \
  --task-id task-pass \
  --worker claude \
  --worktree /home/lingfeng/worktrees/task-pass \
  --prompt-file "$delegation_root/task-pass/prompt.md" \
  --summary "dispatch task-pass"

bash "$scripts_dir/record-delegation-event.sh" \
  --session-id "$session_id" \
  --delegation-root "$delegation_root" \
  --event-type review.passed \
  --task-id task-pass \
  --phase review \
  --status PASS \
  --response-file "$delegation_root/task-pass/response.md" \
  --summary "review passed"

bash "$scripts_dir/record-delegation-event.sh" \
  --session-id "$session_id" \
  --delegation-root "$delegation_root" \
  --event-type review_rejected \
  --task-id task-fail \
  --phase review \
  --status NEEDS_FIX \
  --prompt-file "$delegation_root/task-fail/prompt.md" \
  --response-file "$delegation_root/task-fail/response.md" \
  --codex-takeover \
  --summary "contract failure, Codex takeover"

summary_output="$(bash "$scripts_dir/summarize-delegation-session.sh" --session-id "$session_id" --delegation-root "$delegation_root")"
summary_json="$delegation_root/_sessions/$session_id/delegation-session-summary.json"
summary_md="$delegation_root/_sessions/$session_id/delegation-session-summary.md"

python3 -m json.tool "$summary_json" >/dev/null
grep -q "Delegation Session Summary" "$summary_md"
grep -q "Token savings verdict" <<<"$summary_output"

python3 - <<'PY' "$summary_json"
import json
import sys

with open(sys.argv[1], encoding="utf-8") as fh:
    data = json.load(fh)

assert data["delegated_task_count"] == 2
assert data["worker_success_count"] == 1
assert data["worker_failure_count"] == 1
assert data["contract_failure_count"] == 1
assert data["codex_takeover_count"] == 1
assert data["review_reject_count"] == 1
assert data["estimated_claude_prompt_tokens"] > 0
assert data["estimated_claude_response_tokens"] > 0
assert data["estimated_codex_overhead_tokens"] > 0
assert data["token_savings_verdict"] == "no"
assert data["confidence"] in {"medium", "low"}
assert data["token_metrics_source"] == "estimate"
assert data["quality_gate_results"]
assert data["root_cause_hypotheses"]
assert data["candidate_lessons"]
assert data["candidate_lessons"][0]["candidate_lesson"]
PY

empty_session="session-empty"
bash "$scripts_dir/summarize-delegation-session.sh" --session-id "$empty_session" --delegation-root "$delegation_root" >/dev/null
python3 - <<'PY' "$delegation_root/_sessions/$empty_session/delegation-session-summary.json"
import json
import sys

with open(sys.argv[1], encoding="utf-8") as fh:
    data = json.load(fh)

assert data["delegated_task_count"] == 0
assert data["token_savings_verdict"] == "uncertain"
assert data["confidence"] == "low"
assert data["root_cause_hypotheses"][0]["repairability"] == "monitoring"
PY

echo "session reflection tests passed"
