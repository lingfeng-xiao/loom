#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd -P)"
scripts_dir="$repo_root/.agents/skills/delegate-to-omc/scripts"
tmp_root="$(mktemp -d)"
trap 'rm -rf "$tmp_root"' EXIT

python3 -m py_compile "$scripts_dir/generate-delegation-user-report.py"

delegation_root="$tmp_root/delegations"
session_id="v2-telemetry"
session_root="$delegation_root/_sessions/$session_id"
mkdir -p "$session_root"
cat > "$session_root/events.jsonl" <<'JSONL'
{"session_id":"v2-telemetry","event_type":"review_rejected","task_id":"task-a","status":"NEEDS_FIX","summary":"contract failure","estimated_prompt_tokens":10,"estimated_response_tokens":20,"codex_takeover":true}
JSONL
cat > "$tmp_root/usage.json" <<'JSON'
{"prompt_tokens":123,"response_tokens":45,"cost_usd":0.67}
JSON
bash "$scripts_dir/summarize-delegation-session.sh" --session-id "$session_id" --delegation-root "$delegation_root" --usage-file "$tmp_root/usage.json" >/dev/null
grep -q '"token_metrics_source": "real_usage_file"' "$session_root/delegation-session-summary.json"
grep -q '"estimated_claude_prompt_tokens": 123' "$session_root/delegation-session-summary.json"

cat > "$session_root/session-state.json" <<'JSON'
{"session_id":"v2-telemetry","status":"blocked","tasks":{"task-a":{"state":"gate_failed"}}}
JSON
cat > "$session_root/gate-summary.json" <<'JSON'
{"quality_gate_result":"FAIL","compact_summary":"Machine gate failed: contract.","recommended_action":"small_fix"}
JSON
cp "$session_root/events.jsonl" "$session_root/session-events.jsonl"
python3 "$scripts_dir/generate-delegation-user-report.py" --session-root "$session_root" >/dev/null
grep -q 'Delegation User Report' "$session_root/user-report.md"
grep -q 'Token savings verdict' "$session_root/user-report.md"
grep -q 'Machine gate failed' "$session_root/user-report.md"
grep -q 'task-a' "$session_root/user-report.md"

echo "delegation telemetry report tests passed"
