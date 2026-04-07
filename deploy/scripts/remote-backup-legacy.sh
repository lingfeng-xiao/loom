#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=remote-common.sh
source "$SCRIPT_DIR/remote-common.sh"

legacy_containers="$(list_legacy_containers)"
legacy_networks="$(list_legacy_networks)"

if [[ -z "$legacy_containers" && -z "$legacy_networks" ]]; then
  log "No legacy resources found"
  exit 0
fi

timestamp="$(date +%Y%m%d-%H%M%S)"
backup_dir="$BACKUP_DIR_ROOT/pre-cutover-$timestamp"
tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT

docker ps -a --format 'table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}' > "$tmp_dir/docker-ps.txt" || true
docker network ls > "$tmp_dir/docker-networks.txt" || true
docker volume ls > "$tmp_dir/docker-volumes.txt" || true
ss -ltnp > "$tmp_dir/ports.txt" || true
printf '%s\n' "$legacy_containers" > "$tmp_dir/legacy-containers.txt"
printf '%s\n' "$legacy_networks" > "$tmp_dir/legacy-networks.txt"

while IFS= read -r container; do
  [[ -z "$container" ]] && continue
  docker inspect "$container" > "$tmp_dir/${container}.inspect.json" || true
done <<< "$legacy_containers"

mkdir -p "$backup_dir"
cp -R "$tmp_dir/." "$backup_dir/"
log "Legacy backup saved to $backup_dir"
