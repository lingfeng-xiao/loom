#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd -P)"
scripts_dir="$repo_root/.agents/skills/delegate-to-omc/scripts"
tmp_root="$(mktemp -d)"
trap 'rm -rf "$tmp_root"' EXIT
export PYTHONPYCACHEPREFIX="$tmp_root/pycache"

python3 -m py_compile "$scripts_dir/generate-delegation-user-report.py"

delegation_root="$tmp_root/delegations"
session_id="v2-telemetry"
session_root="$delegation_root/_sessions/$session_id"
mkdir -p "$session_root/tasks/task-a"
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
{"session_id":"v2-telemetry","status":"blocked","claude_worker_enabled":false,"tasks":{"task-a":{"state":"gate_failed"}}}
JSON
cat > "$session_root/gate-summary.json" <<'JSON'
{"quality_gate_result":"FAIL","compact_summary":"Machine gate failed: contract.","recommended_action":"codex_takeover","retry_decision":{"stop_loss":true}}
JSON
cat > "$session_root/env-gate.json" <<'JSON'
{"status":"WARN","issues":[{"code":"shell_environment_error","summary":"zsh risk"}],"auto_recovered":[{"code":"test_artifact_pollution","summary":"Removed __pycache__"}]}
JSON
cat > "$session_root/package-result.json" <<'JSON'
{"status":"PASS","errors":[]}
JSON
cat > "$session_root/tasks/task-a/gate-summary.json" <<'JSON'
{"quality_gate_result":"FAIL","classification":"contract_incomplete"}
JSON
cp "$session_root/events.jsonl" "$session_root/session-events.jsonl"
python3 "$scripts_dir/generate-delegation-user-report.py" --session-root "$session_root" >/dev/null
grep -q 'Delegation User Report' "$session_root/user-report.md"
grep -q 'Token/ROI verdict' "$session_root/user-report.md"
grep -q 'Environment gate' "$session_root/user-report.md"
grep -q 'Artifact packaging' "$session_root/user-report.md"
grep -q 'Claude worker was `disabled`' "$session_root/user-report.md"
grep -q 'test_artifact_pollution' "$session_root/user-report.md"
grep -q 'task-a' "$session_root/user-report.md"

echo "delegation telemetry report tests passed"
