#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=remote-common.sh
source "$SCRIPT_DIR/remote-common.sh"

legacy_containers="$(list_legacy_containers)"
if [[ -z "$legacy_containers" ]]; then
  log "No legacy containers found"
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

while IFS= read -r container; do
  [[ -z "$container" ]] && continue
  docker inspect "$container" > "$tmp_dir/${container}.inspect.json" || true
done <<< "$legacy_containers"

proxy_mount_source="$(docker inspect sprite-proxy --format '{{range .Mounts}}{{if eq .Destination "/etc/nginx/conf.d/default.conf"}}{{.Source}}{{end}}{{end}}' 2>/dev/null || true)"
if [[ -n "$proxy_mount_source" && -f "$proxy_mount_source" ]]; then
  cp "$proxy_mount_source" "$tmp_dir/sprite-proxy-default.conf"
fi

if docker ps -a --format '{{.Names}}' | grep -qx 'sprite-mysql'; then
  sprite_mysql_root_password="$(docker inspect sprite-mysql --format '{{range .Config.Env}}{{println .}}{{end}}' | sed -n 's/^MYSQL_ROOT_PASSWORD=//p' | head -n 1)"
  docker inspect sprite-mysql --format '{{range .Config.Env}}{{println .}}{{end}}' > "$tmp_dir/sprite-mysql.env" || true
  if docker exec sprite-mysql sh -lc 'command -v mysqldump >/dev/null 2>&1'; then
    docker exec -e MYSQL_PWD="$sprite_mysql_root_password" sprite-mysql mysqldump --all-databases -uroot > "$tmp_dir/sprite-mysql-all.sql" 2>/dev/null || \
      docker exec sprite-mysql sh -lc 'mysqldump --all-databases -uroot' > "$tmp_dir/sprite-mysql-all.sql" 2>/dev/null || \
      rm -f "$tmp_dir/sprite-mysql-all.sql"
    [[ -f "$tmp_dir/sprite-mysql-all.sql" ]] && gzip -f "$tmp_dir/sprite-mysql-all.sql"
  fi
fi

mkdir -p "$backup_dir"
cp -R "$tmp_dir/." "$backup_dir/"
log "Legacy backup saved to $backup_dir"
