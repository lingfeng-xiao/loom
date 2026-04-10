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

extract_contract_value() {
  local label="$1"
  local file="$2"
  local line
  line="$(grep -E "^${label}:" "$file" | tail -n 1 || true)"
  if [[ -z "$line" ]]; then
    return 1
  fi

  printf '%s' "${line#${label}:}" | sed 's/^[[:space:]]*//'
}

has_contract_section() {
  local label="$1"
  local file="$2"
  grep -Eq "^${label}:" "$file"
}

has_meaningful_value() {
  local value
  value="$(printf '%s' "$1" | tr -d '\r' | sed 's/^[[:space:]]*//; s/[[:space:]]*$//' | tr '[:upper:]' '[:lower:]')"
  case "$value" in
    ""|"none"|"n/a"|"na"|"not run"|"not-run"|"unknown")
      return 1
      ;;
    *)
      return 0
      ;;
  esac
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
preflight_file="$task_dir/preflight.json"
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
claude -p --setting-sources "user,project" --add-dir "$worktree_path" --permission-mode bypassPermissions --name "delegation-$task_slug" < "$prompt_file"
EOF

exit_code=0
worker_status="DRY_RUN"
declared_result="DRY_RUN"
contract_complete=false
diff_present=false
validation_reported=false
review_required=true
preflight_status="PENDING"

if bash "$script_dir/server-delegation-preflight.sh" --task-id "$task_id" --mode "claude" --repo-root "$repo_root" --worktree-root "$worktree_root" --delegation-root "$delegation_root"; then
  preflight_status="PASS"
else
  preflight_status="FAILED"
fi

if [[ "$dry_run" -eq 1 ]]; then
  printf 'DRY RUN: Claude was not executed.\n' > "$response_file"
  printf 'DRY RUN: worktree not created.\n' > "$status_file"
  printf 'DRY RUN: no diff available.\n' > "$diff_file"
elif [[ "$preflight_status" != "PASS" ]]; then
  exit_code=1
  declared_result="FAILED"
  worker_status="FAILED"
  printf '%s\n' \
    'RESULT: FAILED' \
    'SUMMARY: Delegation preflight failed. Inspect preflight.json and preflight.claude.txt.' \
    'CHANGED_FILES: None' \
    'TESTS_RUN: Preflight only' \
    'RISKS: Remote environment is not ready for live delegation.' \
    'BLOCKERS: Preflight status is FAILED.' \
    'NEXT_ACTIONS: Fix the failing preflight checks and rerun.' > "$response_file"
  printf 'PRECHECK FAILED: live delegation was blocked.\n' > "$status_file"
  printf 'PRECHECK FAILED: no diff available.\n' > "$diff_file"
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

  if (cd "$worktree_path" && claude -p --setting-sources "user,project" --add-dir "$worktree_path" --permission-mode bypassPermissions --name "delegation-$task_slug" < "$prompt_file") >"$response_file" 2>&1; then
    exit_code=0
  else
    exit_code=$?
  fi

  git -C "$worktree_path" status --short > "$status_file" || true
  git -C "$worktree_path" diff --stat > "$diff_file" || true

  if has_contract_section "RESULT" "$response_file" \
    && has_contract_section "SUMMARY" "$response_file" \
    && has_contract_section "CHANGED_FILES" "$response_file" \
    && has_contract_section "TESTS_RUN" "$response_file" \
    && has_contract_section "RISKS" "$response_file" \
    && has_contract_section "BLOCKERS" "$response_file" \
    && has_contract_section "NEXT_ACTIONS" "$response_file"; then
    contract_complete=true
  fi

  if [[ -s "$status_file" || -s "$diff_file" ]]; then
    diff_present=true
  fi

  tests_run_value="$(extract_contract_value "TESTS_RUN" "$response_file" || true)"
  if has_meaningful_value "$tests_run_value"; then
    validation_reported=true
  fi

  declared_result="$(extract_contract_value "RESULT" "$response_file" || true)"
  if [[ $exit_code -ne 0 || "$declared_result" == "FAILED" ]]; then
    worker_status="FAILED"
  elif [[ "$declared_result" == "SUCCESS" && "$contract_complete" == "true" && "$diff_present" == "true" && "$validation_reported" == "true" ]]; then
    worker_status="SUCCESS"
  else
    worker_status="PARTIAL"
  fi
fi

cat > "$result_file" <<EOF
{
  "task_id": "$(json_escape "$task_id")",
  "mode": "claude",
  "branch": "$(json_escape "$branch_name")",
  "remote_repo": "$(json_escape "$repo_root")",
  "worktree": "$(json_escape "$worktree_path")",
  "response_file": "$(json_escape "$response_file")",
  "preflight_file": "$(json_escape "$preflight_file")",
  "preflight_status": "$(json_escape "$preflight_status")",
  "declared_result": "$(json_escape "$declared_result")",
  "contract_complete": $contract_complete,
  "diff_present": $diff_present,
  "validation_reported": $validation_reported,
  "review_required": $review_required,
  "worker_status": "$(json_escape "$worker_status")",
  "exit_code": $exit_code,
  "created_at": "$(json_escape "$created_at")"
}
EOF

echo "Artifacts written to $task_dir"
echo "Result: $worker_status"
