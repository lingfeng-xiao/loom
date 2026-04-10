#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: ./pull-delegation-artifacts.sh --task-id <task-id> [--delegation-root <path>] [--ssh-host <host>] [--remote-delegation-root <path>]"
}

task_id=""
delegation_root=".delegations"
ssh_host="jd"
remote_delegation_root="/home/lingfeng/loom/.delegations"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --task-id) task_id="$2"; shift 2 ;;
    --delegation-root) delegation_root="$2"; shift 2 ;;
    --ssh-host) ssh_host="$2"; shift 2 ;;
    --remote-delegation-root) remote_delegation_root="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage; exit 1 ;;
  esac
done

if [[ -z "$task_id" ]]; then
  usage
  exit 1
fi

command -v ssh >/dev/null 2>&1 || { echo "Required command not found: ssh" >&2; exit 1; }
command -v scp >/dev/null 2>&1 || { echo "Required command not found: scp" >&2; exit 1; }

mkdir -p "$delegation_root/$task_id"
remote_archive="$(ssh "$ssh_host" "mktemp /tmp/loom-delegation.XXXXXX.tar")"
local_archive="$(mktemp "${TMPDIR:-/tmp}/loom-delegation.XXXXXX.tar")"
trap 'rm -f "$local_archive"; if [[ -n "${remote_archive:-}" ]]; then ssh "$ssh_host" "rm -f '\''$remote_archive'\''" >/dev/null 2>&1 || true; fi' EXIT
ssh "$ssh_host" "cd '$remote_delegation_root/$task_id' && tar -cf '$remote_archive' ."
scp "$ssh_host:$remote_archive" "$local_archive"
tar -xf "$local_archive" -C "$delegation_root/$task_id"
rm -f "$local_archive"
ssh "$ssh_host" "rm -f '$remote_archive'" >/dev/null 2>&1 || true
trap - EXIT
echo "Artifacts pulled to $delegation_root/$task_id"
