#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: ./sync-claude-user-config.sh [--source-root <path>] [--ssh-host <host>] [--remote-root <path>] [--include-path <path>]... [--skip-verify]"
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Required command not found: $1" >&2
    exit 1
  }
}

source_root="${HOME}/.claude"
ssh_host="jd"
remote_root="~/.claude"
skip_verify=0
include_paths=(".credentials.json" ".omc-config.json" "settings.json" "settings.local.json")

while [[ $# -gt 0 ]]; do
  case "$1" in
    --source-root) source_root="$2"; shift 2 ;;
    --ssh-host) ssh_host="$2"; shift 2 ;;
    --remote-root) remote_root="$2"; shift 2 ;;
    --include-path) include_paths+=("$2"); shift 2 ;;
    --skip-verify) skip_verify=1; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage; exit 1 ;;
  esac
done

require_cmd ssh
require_cmd scp

source_root="$(cd "$source_root" && pwd -P)"
ssh "$ssh_host" "mkdir -p $remote_root"

copied=0
for relative_path in "${include_paths[@]}"; do
  full_path="$source_root/$relative_path"
  if [[ ! -e "$full_path" ]]; then
    continue
  fi

  remote_path="$remote_root/$relative_path"
  remote_dir="$(dirname "$remote_path")"
  ssh "$ssh_host" "mkdir -p '$remote_dir'"
  scp "$full_path" "${ssh_host}:$remote_path"
  copied=$((copied + 1))
done

if [[ "$copied" -eq 0 ]]; then
  echo "No Claude config files matched the include list under $source_root" >&2
  exit 1
fi

echo "Synced $copied Claude user config file(s) to $ssh_host:$remote_root"

if [[ "$skip_verify" -eq 0 ]]; then
  if ssh "$ssh_host" "export PATH=\$HOME/.npm-global/bin:/usr/local/bin:/usr/bin:/bin:\$PATH; printf 'Reply with CLAUDE_P_OK only.\n' | claude -p --setting-sources 'user,project'"; then
    echo "Remote claude -p verification succeeded."
  else
    echo "Remote claude -p verification failed. Treat delegation as degraded until the preflight checks pass." >&2
    exit 2
  fi
fi
