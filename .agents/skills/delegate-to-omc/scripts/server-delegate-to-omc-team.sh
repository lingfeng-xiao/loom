#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: ./server-delegate-to-omc-team.sh --task-id <task-id> (--task-file <brief.md> | --tasks-dir <dir>) --repo-root <repo> [--base-ref <ref>] [--worktree-root <path>] [--delegation-root <path>] [--timeout-seconds <seconds>] [--idle-timeout-seconds <seconds>] [--dry-run]"
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
timeout_seconds=1800
idle_timeout_seconds=300
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
    --timeout-seconds) timeout_seconds="$2"; shift 2 ;;
    --idle-timeout-seconds) idle_timeout_seconds="$2"; shift 2 ;;
    --dry-run) dry_run=1; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage; exit 1 ;;
  esac
done

if [[ -z "$task_id" || ( -z "$task_file" && -z "$tasks_dir" ) ]]; then
  usage
  exit 1
fi

repo_root="$(cd "$repo_root" && pwd -P)"
[[ -n "$delegation_root" ]] || delegation_root="$repo_root/.delegations"
mkdir -p "$worktree_root" "$delegation_root"
task_dir="$delegation_root/$task_id"
mkdir -p "$task_dir"
subtasks_root="$task_dir/subtasks"
mkdir -p "$subtasks_root"
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"

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
preflight_file="$task_dir/preflight.json"
created_at="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"

printf '# Parallel Claude Delegation\n\nParent task: %s\nRepo root: %s\nBase ref: %s\n\nEach worker must stay inside its assigned worktree and keep changes scoped to its subtask.\n' "$task_id" "$repo_root" "$base_ref" > "$team_prompt"

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
  subtask_dir="$subtasks_root/$subtask_id"
  worker_result_file="$subtask_dir/worker.result.md"
  mkdir -p "$subtask_dir"

  printf '\n## Subtask: %s\nAssigned worktree: %s\nAssigned branch: %s\nRequired result file: %s\n\n' "$subtask_id" "$worktree_path" "$branch_name" "$worker_result_file" >> "$team_prompt"
  cat "$source_path" >> "$team_prompt"
  printf '\n' >> "$team_prompt"

  printf '  {"task_id":"%s","task_file":"%s","branch":"%s","worktree":"%s","worker_result_file":"%s"}' \
    "$(json_escape "$subtask_id")" \
    "$(json_escape "$source_path")" \
    "$(json_escape "$branch_name")" \
    "$(json_escape "$worktree_path")" \
    "$(json_escape "$worker_result_file")" >> "$worktrees_file"
  if [[ "$i" -lt $((${#task_paths[@]} - 1)) ]]; then
    printf ',\n' >> "$worktrees_file"
  else
    printf '\n' >> "$worktrees_file"
  fi
done
printf ']\n' >> "$worktrees_file"

{
  printf '# Parallel Claude Worker Commands\n\n'
  for source_path in "${task_paths[@]}"; do
    subtask_id="$task_id"
    if [[ ${#task_paths[@]} -gt 1 ]]; then
      subtask_id="$task_id-$(basename "$source_path" .md)"
    fi
    printf './.agents/skills/delegate-to-omc/scripts/server-delegate-to-claude.sh --task-id %q --task-file %q --repo-root %q --base-ref %q --worktree-root %q --delegation-root %q --timeout-seconds %q --idle-timeout-seconds %q\n' \
      "$subtask_id" "$source_path" "$repo_root" "$base_ref" "$worktree_root" "$subtasks_root" "$timeout_seconds" "$idle_timeout_seconds"
  done
} > "$command_file"

exit_code=0
worker_status="DRY_RUN"
preflight_status="PENDING"
review_required=true

if bash "$script_dir/server-delegation-preflight.sh" --task-id "$task_id" --mode "team" --repo-root "$repo_root" --worktree-root "$worktree_root" --delegation-root "$delegation_root"; then
  preflight_status="PASS"
else
  preflight_status="FAILED"
fi

if [[ "$dry_run" -eq 1 ]]; then
  printf 'DRY RUN: parallel Claude workers were not executed.\n' > "$response_file"
  printf 'DRY RUN: no team worktrees created.\n' > "$status_file"
  printf 'DRY RUN: no diff available.\n' > "$diff_file"
elif [[ "$preflight_status" != "PASS" ]]; then
  exit_code=1
  worker_status="FAILED"
  printf '%s\n' \
    'RESULT: FAILED' \
    'SUMMARY: Delegation preflight failed. Inspect preflight.json and preflight.claude.txt.' \
    'CHANGED_FILES: None' \
    'TESTS_RUN: Preflight only' \
    'RISKS: Remote parallel Claude environment is not ready.' \
    'BLOCKERS: Preflight status is FAILED.' \
    'NEXT_ACTIONS: Fix the failing preflight checks and rerun.' > "$response_file"
  printf 'PRECHECK FAILED: team delegation was blocked.\n' > "$status_file"
  printf 'PRECHECK FAILED: no diff available.\n' > "$diff_file"
else
  : > "$response_file"
  : > "$status_file"
  : > "$diff_file"
  declare -a pids=()
  for source_path in "${task_paths[@]}"; do
    subtask_id="$task_id"
    if [[ ${#task_paths[@]} -gt 1 ]]; then
      subtask_id="$task_id-$(basename "$source_path" .md)"
    fi

    if [[ "$dry_run" -eq 1 ]]; then
      :
    else
      (
        bash "$script_dir/server-delegate-to-claude.sh" \
          --task-id "$subtask_id" \
          --task-file "$source_path" \
          --repo-root "$repo_root" \
          --base-ref "$base_ref" \
          --worktree-root "$worktree_root" \
          --delegation-root "$subtasks_root" \
          --timeout-seconds "$timeout_seconds" \
          --idle-timeout-seconds "$idle_timeout_seconds"
      ) > "$subtasks_root/$subtask_id/runner.log" 2>&1 &
      pids+=("$!")
    fi
  done

  wait_failed=0
  for pid in "${pids[@]}"; do
    if ! wait "$pid"; then
      wait_failed=1
    fi
  done
  if [[ $wait_failed -ne 0 ]]; then
    exit_code=1
  fi

  success_count=0
  failed_count=0
  partial_count=0
  for source_path in "${task_paths[@]}"; do
    subtask_id="$task_id"
    if [[ ${#task_paths[@]} -gt 1 ]]; then
      subtask_id="$task_id-$(basename "$source_path" .md)"
    fi

    subtask_dir="$subtasks_root/$subtask_id"
    subtask_status_file="$subtask_dir/git.status.txt"
    subtask_diff_file="$subtask_dir/git.diff.stat.txt"
    subtask_result_file="$subtask_dir/result.json"
    runner_log="$subtask_dir/runner.log"
    subtask_worker_status="FAILED"

    if [[ -f "$subtask_result_file" ]]; then
      subtask_worker_status="$(python3 - <<'PY' "$subtask_result_file"
import json, sys
print(json.load(open(sys.argv[1], encoding='utf-8')).get('worker_status', 'FAILED'))
PY
)"
    fi

    case "$subtask_worker_status" in
      SUCCESS) success_count=$((success_count + 1)) ;;
      FAILED) failed_count=$((failed_count + 1)) ;;
      *) partial_count=$((partial_count + 1)) ;;
    esac

    {
      printf '## %s\n' "$subtask_id"
      if [[ -f "$subtask_status_file" ]]; then cat "$subtask_status_file"; else printf 'Missing %s\n' "$subtask_status_file"; fi
      printf '\n'
    } >> "$status_file"
    {
      printf '## %s\n' "$subtask_id"
      if [[ -f "$subtask_diff_file" ]]; then cat "$subtask_diff_file"; else printf 'Missing %s\n' "$subtask_diff_file"; fi
      printf '\n'
    } >> "$diff_file"
    {
      printf '## %s\n' "$subtask_id"
      if [[ -f "$runner_log" ]]; then cat "$runner_log"; fi
      printf '\n'
    } >> "$response_file"
  done

  if [[ $exit_code -ne 0 || $failed_count -gt 0 ]]; then
    worker_status="FAILED"
  elif [[ $success_count -eq ${#task_paths[@]} && $partial_count -eq 0 ]]; then
    worker_status="SUCCESS"
  else
    worker_status="PARTIAL"
  fi
fi

cat > "$result_file" <<EOF
{
  "task_id": "$(json_escape "$task_id")",
  "mode": "team",
  "launcher": "parallel-claude",
  "branch": "$(json_escape "delegation/$task_slug")",
  "remote_repo": "$(json_escape "$repo_root")",
  "worktree": "$(json_escape "$worktree_root")",
  "response_file": "$(json_escape "$response_file")",
  "preflight_file": "$(json_escape "$preflight_file")",
  "preflight_status": "$(json_escape "$preflight_status")",
  "review_required": $review_required,
  "timeout_seconds": $timeout_seconds,
  "idle_timeout_seconds": $idle_timeout_seconds,
  "subtasks_root": "$(json_escape "$subtasks_root")",
  "subtask_count": ${#task_paths[@]},
  "worker_status": "$(json_escape "$worker_status")",
  "exit_code": $exit_code,
  "created_at": "$(json_escape "$created_at")"
}
EOF

echo "Artifacts written to $task_dir"
echo "Result: $worker_status"
