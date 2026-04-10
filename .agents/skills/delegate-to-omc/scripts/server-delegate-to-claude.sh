#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: ./server-delegate-to-claude.sh --task-id <task-id> --task-file <brief.md> --repo-root <repo> [--base-ref <ref>] [--worktree-root <path>] [--delegation-root <path>] [--dry-run]"
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Required command not found: $1" >&2
    exit 1
  }
}

json_escape() {
  printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g'
}

export PATH="$HOME/.npm-global/bin:/usr/local/bin:/usr/bin:/bin:$PATH"

task_id=""
task_file=""
repo_root="$(pwd -P)"
base_ref="main"
worktree_root="/home/lingfeng/worktrees"
delegation_root=""
dry_run=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --task-id) task_id="$2"; shift 2 ;;
    --task-file) task_file="$2"; shift 2 ;;
    --repo-root) repo_root="$2"; shift 2 ;;
    --base-ref) base_ref="$2"; shift 2 ;;
    --worktree-root) worktree_root="$2"; shift 2 ;;
    --delegation-root) delegation_root="$2"; shift 2 ;;
    --dry-run) dry_run=1; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage; exit 1 ;;
  esac
done

if [[ -z "$task_id" || -z "$task_file" ]]; then
  usage
  exit 1
fi

require_cmd git
require_cmd claude

repo_root="$(cd "$repo_root" && pwd -P)"
task_file="$(cd "$(dirname "$task_file")" && pwd -P)/$(basename "$task_file")"
[[ -n "$delegation_root" ]] || delegation_root="$repo_root/.delegations"
mkdir -p "$worktree_root" "$delegation_root"
task_dir="$delegation_root/$task_id"
mkdir -p "$task_dir"
brief_output="$task_dir/brief.md"
if [[ "$task_file" != "$brief_output" ]]; then
  cp "$task_file" "$brief_output"
fi
task_file="$brief_output"

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
skill_root="$(cd "$script_dir/.." && pwd -P)"
prompt_template="$skill_root/assets/paste-free-handoff-template.md"
task_slug="$(printf '%s' "$task_id" | tr '[:upper:]' '[:lower:]' | sed 's/[^a-z0-9._-]/-/g; s/^-*//; s/-*$//')"
branch_name="delegation/$task_slug"
worktree_path="$worktree_root/$task_id"
prompt_file="$task_dir/prompt.sent.md"
response_file="$task_dir/claude.response.md"
status_file="$task_dir/git.status.txt"
diff_file="$task_dir/git.diff.stat.txt"
result_file="$task_dir/result.json"
command_file="$task_dir/command.preview.txt"
created_at="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"

{
  sed \
    -e "s#{{TASK_ID}}#$task_id#g" \
    -e "s#{{WORKTREE}}#$worktree_path#g" \
    -e "s#{{REPO_ROOT}}#$repo_root#g" \
    -e "s#{{TASK_FILE}}#$task_file#g" \
    "$prompt_template"
  printf '\n\n'
  cat "$task_file"
} > "$prompt_file"

cat > "$command_file" <<EOF
cd "$worktree_path"
claude -p --setting-sources "user,project" --add-dir "$worktree_path" --permission-mode bypassPermissions --name "delegation-$task_slug" <prompt from "$prompt_file">
EOF

exit_code=0
worker_status="DRY_RUN"

if [[ "$dry_run" -eq 1 ]]; then
  printf 'DRY RUN: Claude was not executed.\n' > "$response_file"
  printf 'DRY RUN: worktree not created.\n' > "$status_file"
  printf 'DRY RUN: no diff available.\n' > "$diff_file"
else
  if [[ -e "$worktree_path" && ! -e "$worktree_path/.git" ]]; then
    echo "Worktree path exists but is not a git worktree: $worktree_path" >&2
    exit 1
  fi

  if [[ ! -e "$worktree_path" ]]; then
    if git -C "$repo_root" rev-parse --verify "$branch_name" >/dev/null 2>&1; then
      git -C "$repo_root" worktree add "$worktree_path" "$branch_name"
    else
      git -C "$repo_root" worktree add -b "$branch_name" "$worktree_path" "$base_ref"
    fi
  fi

  prompt_text="$(cat "$prompt_file")"
  if (cd "$worktree_path" && claude -p --setting-sources "user,project" --add-dir "$worktree_path" --permission-mode bypassPermissions --name "delegation-$task_slug" "$prompt_text") >"$response_file" 2>&1; then
    exit_code=0
  else
    exit_code=$?
  fi

  git -C "$worktree_path" status --short > "$status_file" || true
  git -C "$worktree_path" diff --stat > "$diff_file" || true

  if grep -Eq '^RESULT:\s*(SUCCESS|PARTIAL|FAILED)\s*$' "$response_file"; then
    worker_status="$(grep -E '^RESULT:\s*(SUCCESS|PARTIAL|FAILED)\s*$' "$response_file" | tail -n 1 | sed 's/^RESULT:[[:space:]]*//')"
  elif [[ $exit_code -ne 0 ]]; then
    worker_status="FAILED"
  else
    worker_status="PARTIAL"
  fi

  if [[ "$worker_status" == "SUCCESS" && ! -s "$status_file" && ! -s "$diff_file" ]]; then
    worker_status="PARTIAL"
  fi
fi

cat > "$result_file" <<EOF
{
  "task_id": "$(json_escape "$task_id")",
  "branch": "$(json_escape "$branch_name")",
  "worktree": "$(json_escape "$worktree_path")",
  "response_file": "$(json_escape "$response_file")",
  "worker_status": "$(json_escape "$worker_status")",
  "exit_code": $exit_code,
  "created_at": "$(json_escape "$created_at")"
}
EOF

echo "Artifacts written to $task_dir"
echo "Result: $worker_status"
