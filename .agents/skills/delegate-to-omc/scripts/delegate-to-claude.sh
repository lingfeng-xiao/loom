#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: ./delegate-to-claude.sh --task-id <task-id> --task-file <brief.md> [--repo-root <repo>] [--base-ref <ref>] [--delegation-root <path>] [--ssh-host <host>] [--remote-repo-root <path>] [--remote-worktree-root <path>] [--remote-delegation-root <path>] [--timeout-seconds <seconds>] [--idle-timeout-seconds <seconds>] [--skip-ensure-claude-ready] [--force-sync-claude-config] [--dry-run]"
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Required command not found: $1" >&2
    exit 1
  }
}

task_id=""
task_file=""
repo_root="$(pwd -P)"
base_ref="main"
delegation_root=""
ssh_host="jd"
remote_repo_root="/home/lingfeng/loom"
remote_worktree_root="/home/lingfeng/worktrees"
remote_delegation_root="/home/lingfeng/loom/.delegations"
timeout_seconds=1800
idle_timeout_seconds=300
skip_ensure_claude_ready=0
force_sync_claude_config=0
dry_run=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --task-id) task_id="$2"; shift 2 ;;
    --task-file) task_file="$2"; shift 2 ;;
    --repo-root) repo_root="$2"; shift 2 ;;
    --base-ref) base_ref="$2"; shift 2 ;;
    --delegation-root) delegation_root="$2"; shift 2 ;;
    --ssh-host) ssh_host="$2"; shift 2 ;;
    --remote-repo-root) remote_repo_root="$2"; shift 2 ;;
    --remote-worktree-root) remote_worktree_root="$2"; shift 2 ;;
    --remote-delegation-root) remote_delegation_root="$2"; shift 2 ;;
    --timeout-seconds) timeout_seconds="$2"; shift 2 ;;
    --idle-timeout-seconds) idle_timeout_seconds="$2"; shift 2 ;;
    --skip-ensure-claude-ready) skip_ensure_claude_ready=1; shift ;;
    --force-sync-claude-config) force_sync_claude_config=1; shift ;;
    --dry-run) dry_run=1; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage; exit 1 ;;
  esac
done

if [[ -z "$task_id" || -z "$task_file" ]]; then
  usage
  exit 1
fi

require_cmd ssh
require_cmd base64

if [[ "$skip_ensure_claude_ready" -eq 0 ]]; then
  ensure_args=(--ssh-host "$ssh_host" --remote-repo-root "$remote_repo_root")
  if [[ "$force_sync_claude_config" -eq 1 ]]; then
    ensure_args+=(--force-sync)
  fi
  "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)/ensure-remote-claude-ready.sh" "${ensure_args[@]}"
fi

repo_root="$(cd "$repo_root" && pwd -P)"
task_file="$(cd "$(dirname "$task_file")" && pwd -P)/$(basename "$task_file")"
[[ -n "$delegation_root" ]] || delegation_root="$repo_root/.delegations"
mkdir -p "$delegation_root/$task_id"
brief_output="$delegation_root/$task_id/brief.md"
if [[ "$task_file" != "$brief_output" ]]; then
  cp "$task_file" "$brief_output"
fi

remote_task_dir="$remote_delegation_root/$task_id"
remote_brief_path="$remote_task_dir/brief.md"
command_file="$delegation_root/$task_id/command.preview.txt"
dry_arg=""
if [[ "$dry_run" -eq 1 ]]; then
  dry_arg="--dry-run"
fi

remote_command="export PATH=\$HOME/.npm-global/bin:/usr/local/bin:/usr/bin:/bin:\$PATH; cd '$remote_repo_root'; bash './.agents/skills/delegate-to-omc/scripts/server-delegate-to-claude.sh' --task-id '$task_id' --task-file '$remote_brief_path' --repo-root '$remote_repo_root' --base-ref '$base_ref' --worktree-root '$remote_worktree_root' --delegation-root '$remote_delegation_root' --timeout-seconds '$timeout_seconds' --idle-timeout-seconds '$idle_timeout_seconds' $dry_arg"
printf 'ssh %q "%s"\n' "$ssh_host" "$remote_command" > "$command_file"

base64 "$brief_output" | ssh "$ssh_host" "mkdir -p '$remote_task_dir' && base64 -d > '$remote_brief_path'"
ssh "$ssh_host" "$remote_command" || true
"$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)/pull-delegation-artifacts.sh" --task-id "$task_id" --delegation-root "$delegation_root" --ssh-host "$ssh_host" --remote-delegation-root "$remote_delegation_root"
echo "Pulled remote artifacts into $delegation_root/$task_id"
