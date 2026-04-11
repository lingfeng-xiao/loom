#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: ./close-delegation.sh --task-id <task-id> [--delegation-root <path>] [--release-id <id>] [--rollback-ref <ref>]"
}

json_escape() {
  printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g'
}

task_id=""
delegation_root=".delegations"
release_id=""
rollback_ref=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --task-id) task_id="$2"; shift 2 ;;
    --delegation-root) delegation_root="$2"; shift 2 ;;
    --release-id) release_id="$2"; shift 2 ;;
    --rollback-ref) rollback_ref="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage; exit 1 ;;
  esac
done

if [[ -z "$task_id" ]]; then
  usage
  exit 1
fi

task_dir="$(cd "$delegation_root" && pwd -P)/$task_id"
review_path="$task_dir/review-notes.md"
result_path="$task_dir/result.json"
preflight_path="$task_dir/preflight.json"
closeout_path="$task_dir/closeout.json"

[[ -f "$review_path" ]] || { echo "Review notes not found: $review_path" >&2; exit 1; }
[[ -f "$result_path" ]] || { echo "Result file not found: $result_path" >&2; exit 1; }
[[ -f "$preflight_path" ]] || { echo "Preflight file not found: $preflight_path" >&2; exit 1; }

review_result="$(grep -E '^REVIEW_RESULT:' "$review_path" | tail -n 1 | sed 's/^REVIEW_RESULT:[[:space:]]*//')"
worker_status="$(python3 - <<'PY' "$result_path"
import json, sys
print(json.load(open(sys.argv[1], encoding="utf-8"))["worker_status"])
PY
)"
preflight_status="$(python3 - <<'PY' "$preflight_path"
import json, sys
print(json.load(open(sys.argv[1], encoding="utf-8"))["status"])
PY
)"

closeable=false
final_state="BLOCKED"
if [[ "$review_result" == "PASS" && "$worker_status" == "SUCCESS" && "$preflight_status" == "PASS" ]]; then
  closeable=true
  final_state="CLOSED"
fi

cat > "$closeout_path" <<EOF
{
  "task_id": "$(json_escape "$task_id")",
  "review_result": "$(json_escape "$review_result")",
  "worker_status": "$(json_escape "$worker_status")",
  "preflight_status": "$(json_escape "$preflight_status")",
  "closeable": $closeable,
  "closed": $closeable,
  "final_state": "$(json_escape "$final_state")",
  "release_id": "$(json_escape "$release_id")",
  "rollback_ref": "$(json_escape "$rollback_ref")",
  "review_file": "$(json_escape "$review_path")",
  "result_file": "$(json_escape "$result_path")",
  "preflight_file": "$(json_escape "$preflight_path")",
  "closed_at": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
}
EOF

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
bash "$script_dir/generate-workflow-report.sh" \
  --task-id "$task_id" \
  --delegation-root "$delegation_root" \
  --release-id "$release_id" \
  --rollback-ref "$rollback_ref"

if [[ "$closeable" != "true" ]]; then
  echo "Delegation cannot be closed until REVIEW_RESULT=PASS, worker_status=SUCCESS, and preflight.status=PASS." >&2
  exit 1
fi

echo "Closeout written to $closeout_path"
