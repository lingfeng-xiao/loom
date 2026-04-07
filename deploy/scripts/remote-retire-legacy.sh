#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=remote-common.sh
source "$SCRIPT_DIR/remote-common.sh"

sprite_mysql_volumes="$(docker inspect sprite-mysql --format '{{range .Mounts}}{{if eq .Type "volume"}}{{println .Name}}{{end}}{{end}}' 2>/dev/null || true)"
proxy_mount_source="$(docker inspect sprite-proxy --format '{{range .Mounts}}{{if eq .Destination "/etc/nginx/conf.d/default.conf"}}{{.Source}}{{end}}{{end}}' 2>/dev/null || true)"
legacy_containers="$(list_legacy_containers)"

if [[ -n "$legacy_containers" ]]; then
  while IFS= read -r container; do
    [[ -z "$container" ]] && continue
    docker rm -f "$container" >/dev/null 2>&1 || true
  done <<< "$legacy_containers"
fi

while IFS= read -r volume_name; do
  [[ -z "$volume_name" ]] && continue
  docker volume rm "$volume_name" >/dev/null 2>&1 || true
done <<< "$sprite_mysql_volumes"

while IFS= read -r network_name; do
  [[ -z "$network_name" ]] && continue
  docker network rm "$network_name" >/dev/null 2>&1 || true
done < <(docker network ls --format '{{.Name}}' | grep -E "$LEGACY_NETWORK_PATTERN" || true)

if [[ -n "$proxy_mount_source" ]]; then
  root_shell "rm -f '$proxy_mount_source'" || true
fi

exited_containers="$(docker ps -aq -f status=exited)"
if [[ -n "$exited_containers" ]]; then
  docker rm -f $exited_containers >/dev/null 2>&1 || true
fi

log "Legacy containers retired"
