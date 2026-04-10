#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: ./delegate-to-omc-team.sh --task-id <task-id> (--task-file <brief.md> | --tasks-dir <dir>) [--repo-root <repo>] [--base-ref <ref>] [--delegation-root <path>] [--ssh-host <host>] [--remote-repo-root <path>] [--remote-worktree-root <path>] [--remote-delegation-root <path>] [--dry-run]"
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Required command not found: $1" >&2
    exit 1
  }
}

task_id=""
task_file=""
tasks_dir=""
repo_root="$(pwd -P)"
base_ref="main"
delegation_root=""
ssh_host="jd"
remote_repo_root="/home/lingfeng/loom"
remote_worktree_root="/home/lingfeng/worktrees"
remote_delegation_root="/home/lingfeng/loom/.delegations"
dry_run=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --task-id) task_id="$2"; shift 2 ;;
    --task-file) task_file="$2"; shift 2 ;;
    --tasks-dir) tasks_dir="$2"; shift 2 ;;
    --repo-root) repo_root="$2"; shift 2 ;;
    --base-ref) base_ref="$2"; shift 2 ;;
    --delegation-root) delegation_root="$2"; shift 2 ;;
    --ssh-host) ssh_host="$2"; shift 2 ;;
    --remote-repo-root) remote_repo_root="$2"; shift 2 ;;
    --remote-worktree-root) remote_worktree_root="$2"; shift 2 ;;
    --remote-delegation-root) remote_delegation_root="$2"; shift 2 ;;
    --dry-run) dry_run=1; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage; exit 1 ;;
  esac
done

if [[ -z "$task_id" || ( -z "$task_file" && -z "$tasks_dir" ) ]]; then
  usage
  exit 1
fi

require_cmd ssh
require_cmd tar
require_cmd base64

repo_root="$(cd "$repo_root" && pwd -P)"
[[ -n "$delegation_root" ]] || delegation_root="$repo_root/.delegations"
mkdir -p "$delegation_root/$task_id"
task_dir="$delegation_root/$task_id"
remote_task_dir="$remote_delegation_root/$task_id"
remote_tasks_root="$remote_task_dir/tasks"

if [[ -n "$task_file" ]]; then
  task_file="$(cd "$(dirname "$task_file")" && pwd -P)/$(basename "$task_file")"
  cp "$task_file" "$task_dir/brief.md"
  base64 "$task_file" | ssh "$ssh_host" "mkdir -p '$remote_task_dir' && base64 -d > '$remote_task_dir/brief.md'"
else
  tasks_dir="$(cd "$tasks_dir" && pwd -P)"
  ssh "$ssh_host" "rm -rf '$remote_tasks_root' && mkdir -p '$remote_tasks_root'"
  tar -cf - -C "$tasks_dir" . | ssh "$ssh_host" "tar -xf - -C '$remote_tasks_root'"
fi

dry_arg=""
if [[ "$dry_run" -eq 1 ]]; then
  dry_arg="--dry-run"
fi

remote_task_arg="--task-file '$remote_task_dir/brief.md'"
if [[ -n "$tasks_dir" ]]; then
  remote_task_arg="--tasks-dir '$remote_tasks_root'"
fi

remote_command="export PATH=\$HOME/.npm-global/bin:/usr/local/bin:/usr/bin:/bin:\$PATH; cd '$remote_repo_root'; bash './.agents/skills/delegate-to-omc/scripts/server-delegate-to-omc-team.sh' --task-id '$task_id' --repo-root '$remote_repo_root' --base-ref '$base_ref' --worktree-root '$remote_worktree_root' --delegation-root '$remote_delegation_root' $remote_task_arg $dry_arg"
printf 'ssh %q "%s"\n' "$ssh_host" "$remote_command" > "$task_dir/command.preview.txt"

ssh "$ssh_host" "$remote_command" || true
"$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)/pull-delegation-artifacts.sh" --task-id "$task_id" --delegation-root "$delegation_root" --ssh-host "$ssh_host" --remote-delegation-root "$remote_delegation_root"
echo "Pulled remote artifacts into $task_dir"
