#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: ./new-delegation.sh --task-id <task-id> --title <short title> [--delegation-root <path>]"
}

task_id=""
title=""
delegation_root=".delegations"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --task-id) task_id="$2"; shift 2 ;;
    --title) title="$2"; shift 2 ;;
    --delegation-root) delegation_root="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage; exit 1 ;;
  esac
done

if [[ -z "$task_id" || -z "$title" ]]; then
  usage
  exit 1
fi

repo_root="$(pwd -P)"
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
skill_root="$(cd "$script_dir/.." && pwd -P)"
delegation_root_abs="$(cd "$repo_root" && mkdir -p "$delegation_root" && cd "$delegation_root" && pwd -P)"
task_dir="$delegation_root_abs/$task_id"

if [[ -e "$task_dir" ]]; then
  echo "Delegation already exists: $task_dir" >&2
  exit 1
fi

mkdir -p "$task_dir"
brief_template="$skill_root/assets/task-brief-template.md"
brief_path="$task_dir/brief.md"
handoff_path="$task_dir/handoff-to-claude.md"
review_path="$task_dir/review-notes.md"
closeout_path="$task_dir/closeout.json"

sed \
  -e "s/{{TASK_ID}}/$task_id/g" \
  -e "s/{{TASK_TITLE}}/$title/g" \
  "$brief_template" > "$brief_path"

cat > "$handoff_path" <<EOF
# Handoff To Claude

Use \`brief.md\` in this folder as the source of truth for task \`$task_id\`.

Suggested next step:

\`\`\`bash
./.agents/skills/delegate-to-omc/scripts/delegate-to-claude.sh --task-id "$task_id" --task-file "$brief_path"
\`\`\`
EOF

cat > "$review_path" <<'EOF'
# Review Notes

REVIEW_RESULT: PENDING

## Scope check

- TODO

## Validation check

- TODO

## Risk check

- TODO

## Minimal fix list

- TODO

## Closeout gate

- Close only after `REVIEW_RESULT: PASS`.
- Use `close-delegation.sh` to write `closeout.json`.
EOF

cat > "$closeout_path" <<EOF
{
  "task_id": "$task_id",
  "review_result": "PENDING",
  "worker_status": "PENDING",
  "preflight_status": "PENDING",
  "closeable": false,
  "closed": false,
  "final_state": "PENDING_REVIEW",
  "release_id": "",
  "rollback_ref": "",
  "review_file": "$review_path",
  "result_file": "$task_dir/result.json",
  "preflight_file": "$task_dir/preflight.json",
  "closed_at": ""
}
EOF

echo "Created delegation scaffold at $task_dir"
echo "Next:"
echo "  ./.agents/skills/delegate-to-omc/scripts/delegate-to-claude.sh --task-id \"$task_id\" --task-file \"$brief_path\""
