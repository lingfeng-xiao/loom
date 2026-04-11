#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd -P)"
scripts_dir="$repo_root/.agents/skills/delegate-to-omc/scripts"
tmp_root="$(mktemp -d)"
trap 'rm -rf "$tmp_root"' EXIT
export PYTHONPYCACHEPREFIX="$tmp_root/pycache"

manifest="$tmp_root/session-manifest.json"
session_root="$tmp_root/session"
cat > "$manifest" <<'JSON'
{
  "session_id": "v2-test",
  "claude_worker_enabled": false,
  "tasks": [
    {"task_id":"task-a","fake_result":"success","worktree":"/home/lingfeng/worktrees/task-a","relevant_files":[".agents/skills/delegate-to-omc/scripts"],"validation":["bash -n"],"done_when":["runner writes checkpoint"]},
    {"task_id":"task-b","fake_result":"failure","worktree":"/home/lingfeng/worktrees/task-b","relevant_files":[".agents/skills/delegate-to-omc/tests"],"validation":["bash tests"],"done_when":["runner is idempotent"]}
  ]
}
JSON

python3 -m py_compile "$scripts_dir/delegation_session_runner.py"
bash -n "$scripts_dir/run-delegation-session.sh"
set +e
bash "$scripts_dir/run-delegation-session.sh" --manifest "$manifest" --session-root "$session_root" --dry-run >/dev/null
first_rc=$?
set -e
if [[ "$first_rc" -eq 0 ]]; then
  echo "mixed fake worker session unexpectedly closed" >&2
  exit 1
fi
python3 -m json.tool "$session_root/session-state.json" >/dev/null
first_count="$(wc -l < "$session_root/session-events.jsonl" | tr -d ' ')"
set +e
bash "$scripts_dir/run-delegation-session.sh" --manifest "$manifest" --session-root "$session_root" --dry-run >/dev/null
second_rc=$?
set -e
second_count="$(wc -l < "$session_root/session-events.jsonl" | tr -d ' ')"
if [[ "$first_count" != "$second_count" ]]; then
  echo "resume duplicated events: $first_count -> $second_count" >&2
  exit 1
fi
grep -q '"status": "blocked"' "$session_root/session-state.json"
grep -q '"dispatch_count": 1' "$session_root/session-state.json"
grep -q '"environment_checked"' "$session_root/session-state.json"
test -s "$session_root/user-report.md"

echo "delegation session runner tests passed"
