#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: ./ensure-remote-claude-ready.sh [--ssh-host <host>] [--remote-repo-root <path>] [--source-root <path>] [--remote-root <path>] [--include-path <path>]... [--force-sync]"
}

ssh_host="jd"
remote_repo_root="/home/lingfeng/loom"
source_root="${HOME}/.claude"
remote_root="~/.claude"
force_sync=0
include_paths=(".credentials.json" ".omc-config.json" "settings.json" "settings.local.json")

verify_remote_claude() {
  ssh "$ssh_host" "export PATH=\$HOME/.npm-global/bin:/usr/local/bin:/usr/bin:/bin:\$PATH; cd '$remote_repo_root'; printf 'Reply with CLAUDE_P_OK only.\n' | claude -p --setting-sources 'user,project'" 2>&1
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --ssh-host) ssh_host="$2"; shift 2 ;;
    --remote-repo-root) remote_repo_root="$2"; shift 2 ;;
    --source-root) source_root="$2"; shift 2 ;;
    --remote-root) remote_root="$2"; shift 2 ;;
    --include-path) include_paths+=("$2"); shift 2 ;;
    --force-sync) force_sync=1; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage; exit 1 ;;
  esac
done

if [[ "$force_sync" -eq 0 ]]; then
  verify_output="$(verify_remote_claude || true)"
  if grep -q 'CLAUDE_P_OK' <<<"$verify_output"; then
    echo "Remote claude -p is already ready on $ssh_host. Skipping config sync."
    exit 0
  fi
  echo "Remote claude -p verification failed on $ssh_host. Syncing local Claude config." >&2
fi

sync_args=(--source-root "$source_root" --ssh-host "$ssh_host" --remote-root "$remote_root" --force-sync)
for include_path in "${include_paths[@]}"; do
  sync_args+=(--include-path "$include_path")
done
"$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)/sync-claude-user-config.sh" "${sync_args[@]}"

verify_output="$(verify_remote_claude || true)"
if ! grep -q 'CLAUDE_P_OK' <<<"$verify_output"; then
  echo "Remote claude -p is still unavailable after config sync on $ssh_host." >&2
  echo "$verify_output" >&2
  exit 1
fi

echo "Remote claude -p is ready on $ssh_host."
