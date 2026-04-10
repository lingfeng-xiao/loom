#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: ./server-delegate-to-omc-team.sh --task-id <task-id> (--task-file <brief.md> | --tasks-dir <dir>) --repo-root <repo> [--base-ref <ref>] [--worktree-root <path>] [--delegation-root <path>] [--dry-run]"
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
tasks_dir=""
repo_root="$(pwd -P)"
base_ref="main"
worktree_root="/home/lingfeng/worktrees"
delegation_root=""
dry_run=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --task-id) task_id="$2"; shift 2 ;;
    --task-file) task_file="$2"; shift 2 ;;
    --tasks-dir) tasks_dir="$2"; shift 2 ;;
    --repo-root) repo_root="$2"; shift 2 ;;
    --base-ref) base_ref="$2"; shift 2 ;;
    --worktree-root) worktree_root="$2"; shift 2 ;;
    --delegation-root) delegation_root="$2"; shift 2 ;;
    --dry-run) dry_run=1; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage; exit 1 ;;
  esac
done

if [[ -z "$task_id" || ( -z "$task_file" && -z "$tasks_dir" ) ]]; then
  usage
  exit 1
fi

require_cmd git
require_cmd omc
require_cmd tmux

repo_root="$(cd "$repo_root" && pwd -P)"
[[ -n "$delegation_root" ]] || delegation_root="$repo_root/.delegations"
mkdir -p "$worktree_root" "$delegation_root"
task_dir="$delegation_root/$task_id"
mkdir -p "$task_dir"

task_paths=()
if [[ -n "$tasks_dir" ]]; then
  tasks_dir="$(cd "$tasks_dir" && pwd -P)"
  while IFS= read -r file; do
    task_paths+=("$file")
  done < <(find "$tasks_dir" -maxdepth 1 -type f -name '*.md' | sort)
else
  task_file="$(cd "$(dirname "$task_file")" && pwd -P)/$(basename "$task_file")"
  if [[ "$task_file" != "$task_dir/brief.md" ]]; then
    cp "$task_file" "$task_dir/brief.md"
  fi
  task_paths+=("$task_dir/brief.md")
fi

if [[ ${#task_paths[@]} -eq 0 ]]; then
  echo "No task files found." >&2
  exit 1
fi

task_slug="$(printf '%s' "$task_id" | tr '[:upper:]' '[:lower:]' | sed 's/[^a-z0-9._-]/-/g; s/^-*//; s/-*$//')"
team_prompt="$task_dir/team.prompt.md"
response_file="$task_dir/omc.response.md"
status_file="$task_dir/git.status.txt"
diff_file="$task_dir/git.diff.stat.txt"
result_file="$task_dir/result.json"
worktrees_file="$task_dir/worktrees.json"
command_file="$task_dir/command.preview.txt"
created_at="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"

printf '# OMC Team Delegation\n\nParent task: %s\nRepo root: %s\nBase ref: %s\n\nEach worker must stay inside its assigned worktree and keep changes scoped to its subtask.\n' "$task_id" "$repo_root" "$base_ref" > "$team_prompt"

printf '[\n' > "$worktrees_file"
for i in "${!task_paths[@]}"; do
  source_path="${task_paths[$i]}"
  subtask_id="$task_id"
  if [[ ${#task_paths[@]} -gt 1 ]]; then
    subtask_id="$task_id-$(basename "$source_path" .md)"
  fi
  subtask_slug="$(printf '%s' "$subtask_id" | tr '[:upper:]' '[:lower:]' | sed 's/[^a-z0-9._-]/-/g; s/^-*//; s/-*$//')"
  branch_name="delegation/$subtask_slug"
  worktree_path="$worktree_root/$subtask_id"

  printf '\n## Subtask: %s\nAssigned worktree: %s\nAssigned branch: %s\n\n' "$subtask_id" "$worktree_path" "$branch_name" >> "$team_prompt"
  cat "$source_path" >> "$team_prompt"
  printf '\n' >> "$team_prompt"

  printf '  {"task_id":"%s","task_file":"%s","branch":"%s","worktree":"%s"}' \
    "$(json_escape "$subtask_id")" \
    "$(json_escape "$source_path")" \
    "$(json_escape "$branch_name")" \
    "$(json_escape "$worktree_path")" >> "$worktrees_file"
  if [[ "$i" -lt $((${#task_paths[@]} - 1)) ]]; then
    printf ',\n' >> "$worktrees_file"
  else
    printf '\n' >> "$worktrees_file"
  fi
done
printf ']\n' >> "$worktrees_file"

cat > "$command_file" <<EOF
omc team ${#task_paths[@]}:claude:executor <prompt from "$team_prompt">
Each worker should use its assigned worktree from "$worktrees_file".
EOF

exit_code=0
worker_status="DRY_RUN"

if [[ "$dry_run" -eq 1 ]]; then
  printf 'DRY RUN: omc team was not executed.\n' > "$response_file"
  printf 'DRY RUN: no team worktrees created.\n' > "$status_file"
  printf 'DRY RUN: no diff available.\n' > "$diff_file"
else
  prompt_text="$(cat "$team_prompt")"
  if omc team "${#task_paths[@]}:claude:executor" "$prompt_text" >"$response_file" 2>&1; then
    exit_code=0
  else
    exit_code=$?
  fi
  printf 'Inspect worktrees.json for isolated worktree assignments.\n' > "$status_file"
  printf 'Inspect worker branches and remote worktrees for detailed diffs.\n' > "$diff_file"
  if grep -Eq '^RESULT:\s*(SUCCESS|PARTIAL|FAILED)\s*$' "$response_file"; then
    worker_status="$(grep -E '^RESULT:\s*(SUCCESS|PARTIAL|FAILED)\s*$' "$response_file" | tail -n 1 | sed 's/^RESULT:[[:space:]]*//')"
  elif [[ $exit_code -ne 0 ]]; then
    worker_status="FAILED"
  else
    worker_status="PARTIAL"
  fi
fi

cat > "$result_file" <<EOF
{
  "task_id": "$(json_escape "$task_id")",
  "branch": "$(json_escape "delegation/$task_slug")",
  "worktree": "$(json_escape "$worktree_root")",
  "response_file": "$(json_escape "$response_file")",
  "worker_status": "$(json_escape "$worker_status")",
  "exit_code": $exit_code,
  "created_at": "$(json_escape "$created_at")"
}
EOF

echo "Artifacts written to $task_dir"
echo "Result: $worker_status"
