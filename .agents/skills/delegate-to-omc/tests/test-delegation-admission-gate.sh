#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd -P)"
scripts_dir="$repo_root/.agents/skills/delegate-to-omc/scripts"
tmp_root="$(mktemp -d)"
trap 'rm -rf "$tmp_root"' EXIT
export PYTHONPYCACHEPREFIX="$tmp_root/pycache"

python3 -m py_compile "$scripts_dir/delegation_admission_gate.py"

manifest="$tmp_root/session-manifest.json"
locks="$tmp_root/locks"
mkdir -p "$locks/worktrees"
cat > "$manifest" <<'JSON'
{
  "tasks": [
    {"task_id":"one","worktree":"/home/lingfeng/worktrees/one","relevant_files":["src/shared"],"done_when":["done"],"validation":["test"],"timeout_seconds":10,"idle_timeout_seconds":5},
    {"task_id":"two","worktree":"/home/lingfeng/worktrees/two","relevant_files":["src/shared"],"done_when":["done"],"validation":["test"],"timeout_seconds":10,"idle_timeout_seconds":5}
  ]
}
JSON
touch "$locks/release.lock" "$locks/worktrees/one.lock"
set +e
python3 "$scripts_dir/delegation_admission_gate.py" admission --manifest "$manifest" --locks-root "$locks" --output "$tmp_root/admission.json"
admission_rc=$?
set -e
if [[ "$admission_rc" -eq 0 ]]; then
  echo "admission unexpectedly passed" >&2
  exit 1
fi
grep -q 'file_overlap' "$tmp_root/admission.json"
grep -q 'worktree_lock' "$tmp_root/admission.json"
grep -q 'release_lock' "$tmp_root/admission.json"

task_dir="$tmp_root/task"
mkdir -p "$task_dir"
cat > "$task_dir/result.json" <<'JSON'
{"declared_result":"SUCCESS","worker_status":"Execution error","contract_complete":false,"missing_contract_fields":["RISKS"],"diff_present":false,"validation_reported":false,"timed_out":true,"idle_timed_out":true}
JSON
cat > "$task_dir/preflight.json" <<'JSON'
{"status":"FAIL","issues":[{"code":"baseline_stale"},{"code":"skill_drift"},{"code":"shell_environment_error"},{"code":"test_artifact_pollution"},{"code":"healthcheck_warmup_retry"}]}
JSON
cat > "$task_dir/review-result.json" <<'JSON'
{"review_result":"NEEDS_FIX"}
JSON
cat > "$task_dir/gate-history.json" <<'JSON'
[{"classification":"claude_invocation_error"}]
JSON
set +e
python3 "$scripts_dir/delegation_admission_gate.py" gate --task-dir "$task_dir" --output "$tmp_root/gate-summary.json" --relevant-file allowed
gate_rc=$?
set -e
if [[ "$gate_rc" -eq 0 ]]; then
  echo "gate unexpectedly passed" >&2
  exit 1
fi
python3 -m json.tool "$tmp_root/gate-summary.json" >/dev/null
python3 -m json.tool "$tmp_root/retry-decision.json" >/dev/null
grep -q 'claude_invocation_error' "$tmp_root/gate-summary.json"
grep -q 'baseline_stale' "$tmp_root/gate-summary.json"
grep -q 'skill_drift' "$tmp_root/gate-summary.json"
grep -q '"stop_loss": true' "$tmp_root/retry-decision.json"

with_diff="$tmp_root/with-diff"
mkdir -p "$with_diff"
cat > "$with_diff/result.json" <<'JSON'
{"worker_status":"Execution error","diff_present":true,"contract_complete":true,"validation_reported":true}
JSON
python3 "$scripts_dir/delegation_admission_gate.py" gate --task-dir "$with_diff" --output "$tmp_root/with-diff.json" >/dev/null 2>&1 || true
grep -q 'artifact_needs_review' "$tmp_root/with-diff.json"

scope_dir="$tmp_root/scope"
mkdir -p "$scope_dir"
cat > "$scope_dir/result.json" <<'JSON'
{"declared_result":"PARTIAL","diff_present":true,"contract_complete":true,"validation_reported":true}
JSON
printf ' M outside/file.txt\n' > "$scope_dir/git.status.txt"
python3 "$scripts_dir/delegation_admission_gate.py" gate --task-dir "$scope_dir" --output "$tmp_root/scope.json" --relevant-file allowed >/dev/null 2>&1 || true
grep -q 'scope' "$tmp_root/scope.json"

echo "delegation admission gate tests passed"
