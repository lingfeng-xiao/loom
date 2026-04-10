#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: ./sync-server-mirror.sh [--ssh-host <host>] [--remote-repo-root <path>] [--mirror-root <path>] [--dry-run]"
}

ssh_host="jd"
remote_repo_root="/home/lingfeng/loom"
mirror_root=".mirror/server-head"
dry_run=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --ssh-host) ssh_host="$2"; shift 2 ;;
    --remote-repo-root) remote_repo_root="$2"; shift 2 ;;
    --mirror-root) mirror_root="$2"; shift 2 ;;
    --dry-run) dry_run=1; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage; exit 1 ;;
  esac
done

command -v ssh >/dev/null 2>&1 || { echo "Required command not found: ssh" >&2; exit 1; }
command -v scp >/dev/null 2>&1 || { echo "Required command not found: scp" >&2; exit 1; }

mirror_root="$(mkdir -p "$(dirname "$mirror_root")" && cd "$(dirname "$mirror_root")" && pwd -P)/$(basename "$mirror_root")"
tmp_dir="${mirror_root}.tmp"
rm -rf "$tmp_dir"
mkdir -p "$tmp_dir"

remote_head="$(ssh "$ssh_host" "cd '$remote_repo_root' && git rev-parse HEAD")" || {
  rm -rf "$tmp_dir"
  echo "Remote repo at $remote_repo_root does not have a valid git HEAD. Bootstrap the server repo before syncing the mirror." >&2
  exit 1
}

transport="none"
if [[ "$dry_run" -ne 1 ]]; then
  if command -v rsync >/dev/null 2>&1 && ssh "$ssh_host" "command -v rsync >/dev/null 2>&1"; then
    rsync -a --delete \
      --exclude='.git' \
      --exclude='node_modules' \
      --exclude='target' \
      --exclude='logs' \
      --exclude='vault' \
      --exclude='.env' \
      --exclude='.delegations' \
      --exclude='.release' \
      --exclude='.tmp' \
      --exclude='*.log' \
      -e ssh \
      "$ssh_host:$remote_repo_root/" \
      "$tmp_dir/"
    transport="rsync"
  else
    remote_archive="$(ssh "$ssh_host" "mktemp /tmp/loom-mirror.XXXXXX.tar")"
    local_archive="$(mktemp "${TMPDIR:-/tmp}/loom-mirror.XXXXXX.tar")"
    trap 'rm -f "$local_archive"; if [[ -n "${remote_archive:-}" ]]; then ssh "$ssh_host" "rm -f '\''$remote_archive'\''" >/dev/null 2>&1 || true; fi' EXIT
    ssh "$ssh_host" "cd '$remote_repo_root' && tar --exclude='.git' --exclude='node_modules' --exclude='target' --exclude='logs' --exclude='vault' --exclude='.env' --exclude='.delegations' --exclude='.release' --exclude='.tmp' --exclude='*.log' -cf '$remote_archive' ."
    scp "$ssh_host:$remote_archive" "$local_archive"
    tar -xf "$local_archive" -C "$tmp_dir"
    rm -f "$local_archive"
    ssh "$ssh_host" "rm -f '$remote_archive'" >/dev/null 2>&1 || true
    trap - EXIT
    transport="tar"
  fi
fi

cat > "$tmp_dir/.mirror-meta.json" <<EOF
{
  "remote_repo": "$remote_repo_root",
  "remote_head": "$remote_head",
  "synced_at": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")",
  "mode": "$(if [[ "$dry_run" -eq 1 ]]; then echo dry-run; else echo full-sync; fi)",
  "transport": "$transport"
}
EOF

rm -rf "$mirror_root"
mv "$tmp_dir" "$mirror_root"
echo "Mirror refreshed at $mirror_root"
