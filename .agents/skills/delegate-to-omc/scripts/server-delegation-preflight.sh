#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: ./server-delegation-preflight.sh --task-id <task-id> --mode <claude|team> --repo-root <repo> [--worktree-root <path>] [--delegation-root <path>]"
}

json_escape() {
  printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g'
}

task_id=""
mode=""
repo_root="$(pwd -P)"
worktree_root="/home/lingfeng/worktrees"
delegation_root=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --task-id) task_id="$2"; shift 2 ;;
    --mode) mode="$2"; shift 2 ;;
    --repo-root) repo_root="$2"; shift 2 ;;
    --worktree-root) worktree_root="$2"; shift 2 ;;
    --delegation-root) delegation_root="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage; exit 1 ;;
  esac
done

if [[ -z "$task_id" || -z "$mode" ]]; then
  usage
  exit 1
fi

repo_root="$(cd "$repo_root" && pwd -P)"
[[ -n "$delegation_root" ]] || delegation_root="$repo_root/.delegations"
mkdir -p "$delegation_root"
task_dir="$delegation_root/$task_id"
mkdir -p "$task_dir"
preflight_file="$task_dir/preflight.json"
claude_verify_file="$task_dir/preflight.claude.txt"
checked_at="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"

git_available=false
claude_available=false
claude_verify_passed=false
omc_available=false
tmux_available=false
worktree_root_writable=false
repo_head=""
status="PASS"
failure_reason=""

if command -v git >/dev/null 2>&1; then
  git_available=true
else
  status="FAILED"
  failure_reason="git command not found"
fi

if [[ "$status" == "PASS" ]]; then
  if repo_head="$(git -C "$repo_root" rev-parse HEAD 2>/dev/null)"; then
    :
  else
    status="FAILED"
    failure_reason="remote repo does not have a valid git HEAD"
    repo_head=""
  fi
fi

mkdir -p "$worktree_root" 2>/dev/null || true
tmp_probe=""
if tmp_probe="$(mktemp -d "$worktree_root/.delegation-preflight.XXXXXX" 2>/dev/null)"; then
  worktree_root_writable=true
  rm -rf "$tmp_probe"
else
  status="FAILED"
  failure_reason="${failure_reason:-worktree root is not writable}"
fi

if command -v claude >/dev/null 2>&1; then
  claude_available=true
else
  status="FAILED"
  failure_reason="${failure_reason:-claude command not found}"
fi

if [[ "$claude_available" == "true" ]]; then
  if printf 'Reply with CLAUDE_P_OK only.\n' | env SHELL=/bin/bash claude --print --input-format text --output-format text --setting-sources "user,project" >"$claude_verify_file" 2>&1; then
    if grep -q 'CLAUDE_P_OK' "$claude_verify_file"; then
      claude_verify_passed=true
    else
      status="FAILED"
      failure_reason="${failure_reason:-claude verification did not return CLAUDE_P_OK}"
    fi
  else
    status="FAILED"
    failure_reason="${failure_reason:-claude verification failed}"
  fi
fi

if command -v omc >/dev/null 2>&1; then
  omc_available=true
fi

if command -v tmux >/dev/null 2>&1; then
  tmux_available=true
fi

cat > "$preflight_file" <<EOF
{
  "task_id": "$(json_escape "$task_id")",
  "mode": "$(json_escape "$mode")",
  "remote_repo": "$(json_escape "$repo_root")",
  "remote_head": "$(json_escape "$repo_head")",
  "worktree_root": "$(json_escape "$worktree_root")",
  "delegation_root": "$(json_escape "$delegation_root")",
  "git_available": $git_available,
  "claude_available": $claude_available,
  "claude_verify_passed": $claude_verify_passed,
  "omc_available": $omc_available,
  "tmux_available": $tmux_available,
  "worktree_root_writable": $worktree_root_writable,
  "status": "$(json_escape "$status")",
  "failure_reason": "$(json_escape "$failure_reason")",
  "checked_at": "$(json_escape "$checked_at")",
  "claude_verify_file": "$(json_escape "$claude_verify_file")"
}
EOF

if [[ "$status" != "PASS" ]]; then
  echo "Delegation preflight failed: $failure_reason" >&2
  exit 1
fi
