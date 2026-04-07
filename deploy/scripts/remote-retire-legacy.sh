#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=remote-common.sh
source "$SCRIPT_DIR/remote-common.sh"

legacy_containers="$(list_legacy_containers)"

if [[ -n "$legacy_containers" ]]; then
  while IFS= read -r container; do
    [[ -z "$container" ]] && continue
    docker rm -f "$container" >/dev/null 2>&1 || true
  done <<< "$legacy_containers"
fi

while IFS= read -r network_name; do
  [[ -z "$network_name" ]] && continue
  docker network rm "$network_name" >/dev/null 2>&1 || true
done < <(list_legacy_networks)

exited_containers="$(docker ps -aq -f status=exited)"
if [[ -n "$exited_containers" ]]; then
  docker rm -f $exited_containers >/dev/null 2>&1 || true
fi

log "Legacy containers retired"
