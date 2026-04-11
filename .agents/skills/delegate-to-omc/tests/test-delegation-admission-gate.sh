#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd -P)"
scripts_dir="$repo_root/.agents/skills/delegate-to-omc/scripts"
tmp_root="$(mktemp -d)"
trap 'rm -rf "$tmp_root"' EXIT

python3 -m py_compile "$scripts_dir/delegation_admission_gate.py"

manifest="$tmp_root/session-manifest.json"
locks="$tmp_root/locks"
mkdir -p "$locks/worktrees"
cat > "$manifest" <<'JSON'
{
  "tasks": [
    {
      "task_id": "one",
      "worktree": "/home/lingfeng/worktrees/one",
      "relevant_files": ["src/shared"],
      "done_when": ["done"],
      "validation": ["test"],
      "timeout_seconds": 10,
      "idle_timeout_seconds": 5
    },
    {
      "task_id": "two",
      "worktree": "/home/lingfeng/worktrees/two",
      "relevant_files": ["src/shared"],
      "done_when": ["done"],
      "validation": ["test"],
      "timeout_seconds": 10,
      "idle_timeout_seconds": 5
    }
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
{"declared_result":"SUCCESS","worker_status":"PARTIAL","contract_complete":false,"diff_present":false,"validation_reported":false,"timed_out":true,"idle_timed_out":true}
JSON
cat > "$task_dir/preflight.json" <<'JSON'
{"status":"PASS"}
JSON
cat > "$task_dir/review-result.json" <<'JSON'
{"review_result":"NEEDS_FIX"}
JSON
cat > "$task_dir/git.status.txt" <<'TXT'
 M outside/file.txt
TXT
set +e
python3 "$scripts_dir/delegation_admission_gate.py" gate --task-dir "$task_dir" --output "$tmp_root/gate-summary.json" --relevant-file allowed
gate_rc=$?
set -e
if [[ "$gate_rc" -eq 0 ]]; then
  echo "gate unexpectedly passed" >&2
  exit 1
fi
python3 -m json.tool "$tmp_root/gate-summary.json" >/dev/null
grep -q 'timeout' "$tmp_root/gate-summary.json"
grep -q 'idle_timeout' "$tmp_root/gate-summary.json"
grep -q 'contract' "$tmp_root/gate-summary.json"
grep -q 'scope' "$tmp_root/gate-summary.json"

echo "delegation admission gate tests passed"
