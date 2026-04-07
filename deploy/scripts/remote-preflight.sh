#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=remote-common.sh
source "$SCRIPT_DIR/remote-common.sh"

ALLOW_LEGACY_CONTAINERS="${ALLOW_LEGACY_CONTAINERS:-0}"
ALLOW_LEGACY_NETWORKS="${ALLOW_LEGACY_NETWORKS:-0}"

command -v docker >/dev/null 2>&1 || die "docker is not installed"
docker compose version >/dev/null 2>&1 || die "docker compose is not available"
root_shell "id -u >/dev/null"
root_shell "systemctl is-active docker.service >/dev/null"

[[ -d "$DEPLOY_STAGE_ROOT" ]] || die "Deploy stage root not found: $DEPLOY_STAGE_ROOT"
[[ -w "$DEPLOY_STAGE_ROOT" ]] || die "Deploy stage root is not writable: $DEPLOY_STAGE_ROOT"

install_parent="$(dirname "$INSTALL_ROOT")"
if root_shell "[ -d '$INSTALL_ROOT' ]"; then
  root_shell "[ -w '$INSTALL_ROOT' ]" || die "Install root is not writable: $INSTALL_ROOT"
else
  root_shell "[ -d '$install_parent' ] && [ -w '$install_parent' ]" || die "Install root parent is not writable: $install_parent"
fi

legacy_containers="$(list_legacy_containers)"
if [[ -n "$legacy_containers" && "$ALLOW_LEGACY_CONTAINERS" != "1" ]]; then
  printf '%s\n' "$legacy_containers" >&2
  die "Legacy containers are still present"
fi

legacy_networks="$(list_legacy_networks)"
if [[ -n "$legacy_networks" && "$ALLOW_LEGACY_NETWORKS" != "1" ]]; then
  printf '%s\n' "$legacy_networks" >&2
  die "Legacy Docker networks are still present"
fi

port_80_containers="$(list_port_80_containers | grep -v "^${TEMPLATE_EDGE_CONTAINER_NAME}\$" || true)"
if [[ -n "$port_80_containers" ]]; then
  printf '%s\n' "$port_80_containers" >&2
  die "Port 80 is occupied by non-template containers"
fi

if ! list_port_80_containers | grep -qx "$TEMPLATE_EDGE_CONTAINER_NAME"; then
  if root_shell "ss -ltnH '( sport = :80 )' | grep -q ."; then
    die "Port 80 is occupied by a non-template listener"
  fi
fi

if systemd_unit_exists; then
  current_state="$(systemd_state)"
  if [[ "$current_state" == "failed" ]]; then
    die "Systemd unit $SYSTEMD_UNIT_NAME is in failed state"
  fi
  log "Systemd unit $SYSTEMD_UNIT_NAME state: ${current_state:-inactive}"
else
  log "Systemd unit $SYSTEMD_UNIT_NAME is not installed yet"
fi

if [[ -f "$COMPOSE_FILE" && -f "$ENV_FILE" ]]; then
  compose_with_env "$ENV_FILE" config >/dev/null
else
  log "Skipping compose render check because $COMPOSE_FILE or $ENV_FILE is not available yet"
fi

log "Preflight passed"
