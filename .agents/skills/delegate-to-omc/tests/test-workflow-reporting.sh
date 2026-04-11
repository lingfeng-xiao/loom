#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd -P)"
scripts_dir="$repo_root/.agents/skills/delegate-to-omc/scripts"
tmp_root="$(mktemp -d)"
trap 'rm -rf "$tmp_root"' EXIT

assert_json() {
  python3 -m json.tool "$1" >/dev/null
}

write_common_files() {
  local task_dir="$1"
  local review_result="$2"
  local worker_status="$3"
  local preflight_status="$4"
  local extra_result="$5"
  mkdir -p "$task_dir"
  cat > "$task_dir/review-notes.md" <<EOF
# Review Notes

REVIEW_RESULT: $review_result
EOF
  cat > "$task_dir/result.json" <<EOF
{"worker_status":"$worker_status","declared_result":"SUCCESS","contract_complete":true,"diff_present":true,"validation_reported":true$extra_result}
EOF
  cat > "$task_dir/preflight.json" <<EOF
{"status":"$preflight_status"}
EOF
}

delegation_root="$tmp_root/delegations"
mkdir -p "$delegation_root"

write_common_files "$delegation_root/pass" "PASS" "SUCCESS" "PASS" ""
cat > "$delegation_root/pass/review-result.json" <<'EOF'
{"review_result":"PASS","issues":[],"recommended_corrections":[],"minimal_fix_list":[]}
EOF
bash "$scripts_dir/close-delegation.sh" --task-id pass --delegation-root "$delegation_root" --release-id rel-pass --rollback-ref rollback-pass
assert_json "$delegation_root/pass/workflow-report.json"
grep -q '"final_state": "CLOSED"' "$delegation_root/pass/workflow-report.json"

write_common_files "$delegation_root/blocked" "NEEDS_FIX" "PARTIAL" "PASS" ',"contract_complete":false,"diff_present":false,"validation_reported":false'
cat > "$delegation_root/blocked/review-result.json" <<'EOF'
{"review_result":"NEEDS_FIX","issues":["No real diff was detected.","Fake Claude returned Execution error."],"recommended_corrections":["Make a real edit."],"minimal_fix_list":["Make a real edit."]}
EOF
cat > "$delegation_root/blocked/failure-triage.json" <<'EOF'
{"repair_size":"medium","recommended_action":"medium_fix","confidence":"medium","likely_causes":["prompt_contract_or_worker_runtime"],"failed_gates":["contract","diff"]}
EOF
set +e
bash "$scripts_dir/close-delegation.sh" --task-id blocked --delegation-root "$delegation_root" --release-id rel-blocked --rollback-ref rollback-blocked
blocked_rc=$?
set -e
if [[ "$blocked_rc" -eq 0 ]]; then
  echo "blocked closeout unexpectedly passed" >&2
  exit 1
fi
assert_json "$delegation_root/blocked/workflow-report.json"
grep -q 'No real diff was detected' "$delegation_root/blocked/workflow-report.md"
grep -q 'Fake Claude returned Execution error' "$delegation_root/blocked/workflow-report.md"
grep -q 'Recommended action: `medium_fix`' "$delegation_root/blocked/workflow-report.md"
python3 - <<'PY' "$delegation_root/blocked/workflow-report.json"
import json
import sys

with open(sys.argv[1], encoding="utf-8") as fh:
    data = json.load(fh)

assert data["repair_size"] == "medium"
assert data["recommended_action"] == "medium_fix"
assert data["failure_triage"]["confidence"] == "medium"
PY

write_common_files "$delegation_root/timeout" "NEEDS_FIX" "FAILED" "PASS" ',"timed_out":true,"idle_timed_out":true'
cat > "$delegation_root/timeout/review-result.json" <<'EOF'
{"review_result":"NEEDS_FIX","issues":["Timeout was detected.","Idle timeout was detected."],"recommended_corrections":["Split the task."],"minimal_fix_list":["Split the task."]}
EOF
set +e
bash "$scripts_dir/close-delegation.sh" --task-id timeout --delegation-root "$delegation_root" >/dev/null 2>&1
timeout_rc=$?
set -e
if [[ "$timeout_rc" -eq 0 ]]; then
  echo "timeout closeout unexpectedly passed" >&2
  exit 1
fi
assert_json "$delegation_root/timeout/workflow-report.json"
grep -q 'Worker hit overall timeout' "$delegation_root/timeout/workflow-report.md"
grep -q 'Worker hit idle-output timeout' "$delegation_root/timeout/workflow-report.md"

bash "$scripts_dir/append-issue-telemetry.sh" \
  --task-id blocked \
  --delegation-root "$delegation_root" \
  --event-id issue-1 \
  --workflow-id workflow-test \
  --attempt 1 \
  --phase review \
  --category review_rejected \
  --severity medium \
  --summary 'summary with "quotes" and spaces' \
  --evidence-file "$delegation_root/blocked/workflow-report.json" \
  --follow-up-required \
  --actor codex
python3 - <<'PY' "$delegation_root/blocked/telemetry/events.jsonl"
import json, sys
with open(sys.argv[1], encoding="utf-8") as fh:
    rows = [json.loads(line) for line in fh if line.strip()]
assert rows[0]["summary"] == 'summary with "quotes" and spaces'
assert rows[0]["category"] == "review_rejected"
PY

set +e
bash "$scripts_dir/append-issue-telemetry.sh" \
  --task-id blocked \
  --delegation-root "$delegation_root" \
  --event-id issue-2 \
  --workflow-id workflow-test \
  --attempt 1 \
  --phase review \
  --category bad_category \
  --severity medium \
  --summary bad >/dev/null 2>&1
bad_category_rc=$?
set -e
if [[ "$bad_category_rc" -eq 0 ]]; then
  echo "invalid telemetry category unexpectedly passed" >&2
  exit 1
fi

today="$(date -u +%Y-%m-%d)"
bash "$scripts_dir/daily-rollup-telemetry.sh" --delegation-root "$delegation_root" --observability-root "$tmp_root/observability" --date "$today"
assert_json "$tmp_root/observability/daily/$today.json"
grep -q 'review_rejected' "$tmp_root/observability/daily/$today.json"

mirror_dir="$tmp_root/mirror/server-head"
mkdir -p "$mirror_dir"
cat > "$mirror_dir/.mirror-meta.json" <<'EOF'
{"remote_head":"abc123","remote_repo":"/home/lingfeng/loom"}
EOF
test ! -e "$mirror_dir/.git"
python3 - <<'PY' "$mirror_dir/.mirror-meta.json"
import json, sys
with open(sys.argv[1], encoding="utf-8") as fh:
    data = json.load(fh)
assert data["remote_head"] == "abc123"
PY

echo "workflow reporting tests passed"
