#!/usr/bin/env bash
set -euo pipefail

DEPLOY_STAGE_ROOT="${DEPLOY_STAGE_ROOT:-${DEPLOY_ROOT:-$HOME/template-deploy}}"
INSTALL_ROOT="${INSTALL_ROOT:-/opt/template}"
BUNDLE_ROOT="${BUNDLE_ROOT:-$DEPLOY_STAGE_ROOT/bundle}"
COMPOSE_FILE="${COMPOSE_FILE:-$INSTALL_ROOT/compose/docker-compose.production.yml}"
ENV_FILE="${ENV_FILE:-$INSTALL_ROOT/env/.env.production}"
STATE_DIR="${STATE_DIR:-$INSTALL_ROOT/state}"
BACKUP_DIR_ROOT="${BACKUP_DIR_ROOT:-$INSTALL_ROOT/backups}"
SYSTEMD_UNIT_NAME="${SYSTEMD_UNIT_NAME:-template.service}"
ROOT_HELPER_IMAGE="${ROOT_HELPER_IMAGE:-debian:bookworm-slim}"
DEPLOY_OWNER="${DEPLOY_OWNER:-$(id -un)}"
DEPLOY_GROUP="${DEPLOY_GROUP:-$(id -gn)}"

readonly LEGACY_CONTAINER_PATTERN='^(sprite-proxy|sprite-app|sprite-web-1|sprite-mysql|loom-.*|template-api|template-web)$'
readonly LEGACY_NETWORK_PATTERN='^(sprite-network|sprite_default|loom_default)$'
readonly TEMPLATE_EDGE_CONTAINER_NAME='template-template-edge-1'
readonly COMPOSE_ENV_KEYS=(
  MYSQL_DATABASE
  MYSQL_USER
  MYSQL_PASSWORD
  MYSQL_ROOT_PASSWORD
  TEMPLATE_PUBLIC_PORT
  TEMPLATE_SERVER_PORT
  TEMPLATE_NODE_PORT
  TEMPLATE_SERVER_TOKEN
  TEMPLATE_NODE_HEARTBEAT_TIMEOUT_SECONDS
  TEMPLATE_NODE_NAME
  TEMPLATE_NODE_TYPE
  TEMPLATE_NODE_HOST
  TEMPLATE_NODE_HEARTBEAT_INTERVAL_MS
  TEMPLATE_NODE_HEARTBEAT_INITIAL_DELAY_MS
  TEMPLATE_NODE_STATE_HOST_DIR
  TEMPLATE_SERVER_IMAGE
  TEMPLATE_WEB_IMAGE
  TEMPLATE_NODE_IMAGE
)

log() {
  printf '[template-release] %s\n' "$*"
}

die() {
  log "ERROR: $*" >&2
  exit 1
}

require_file() {
  [[ -f "$1" ]] || die "Missing file: $1"
}

parse_env_file() {
  local file="$1"
  [[ -f "$file" ]] || return 0

  while IFS= read -r line || [[ -n "$line" ]]; do
    line="${line%$'\r'}"
    [[ -z "$line" || "$line" =~ ^[[:space:]]*# ]] && continue

    local key="${line%%=*}"
    local value="${line#*=}"
    key="${key#"${key%%[![:space:]]*}"}"
    key="${key%"${key##*[![:space:]]}"}"

    if [[ "$value" == \"*\" && "$value" == *\" ]]; then
      value="${value:1:${#value}-2}"
    elif [[ "$value" == \'*\' && "$value" == *\' ]]; then
      value="${value:1:${#value}-2}"
    fi

    export "${key}=${value}"
  done < "$file"
}

root_shell() {
  local command="$1"

  if sudo -n true >/dev/null 2>&1; then
    sudo /bin/sh -lc "$command"
    return
  fi

  docker run --rm --privileged --pid=host "$ROOT_HELPER_IMAGE" \
    nsenter -t 1 -m -u -i -n -p /bin/sh -lc "$command"
}

compose_with_env() {
  local env_file="$1"
  shift
  local env_command=(env)

  for key in "${COMPOSE_ENV_KEYS[@]}"; do
    env_command+=("-u" "$key")
  done

  "${env_command[@]}" docker compose -f "$COMPOSE_FILE" --env-file "$env_file" "$@"
}

ensure_root_dirs() {
  root_shell "install -d -m 755 \
    '$INSTALL_ROOT' \
    '$INSTALL_ROOT/compose' \
    '$INSTALL_ROOT/compose/edge' \
    '$INSTALL_ROOT/scripts' \
    '$INSTALL_ROOT/env' \
    '$STATE_DIR' \
    '$BACKUP_DIR_ROOT' \
    '$INSTALL_ROOT/data' \
    '$INSTALL_ROOT/data/node-state'"
  root_shell "chown -R '$DEPLOY_OWNER:$DEPLOY_GROUP' '$INSTALL_ROOT'"
}

sync_bundle_to_install_root() {
  require_file "$BUNDLE_ROOT/compose/docker-compose.production.yml"
  require_file "$BUNDLE_ROOT/compose/edge/nginx.conf"
  require_file "$BUNDLE_ROOT/systemd/template.service"

  ensure_root_dirs

  root_shell "cp '$BUNDLE_ROOT/compose/docker-compose.production.yml' '$INSTALL_ROOT/compose/docker-compose.production.yml'"
  root_shell "cp '$BUNDLE_ROOT/compose/edge/nginx.conf' '$INSTALL_ROOT/compose/edge/nginx.conf'"
  root_shell "cp '$BUNDLE_ROOT/systemd/template.service' '/etc/systemd/system/$SYSTEMD_UNIT_NAME'"
  root_shell "chmod 644 '/etc/systemd/system/$SYSTEMD_UNIT_NAME'"
  root_shell "find '$INSTALL_ROOT/scripts' -maxdepth 1 -type f -name '*.sh' -delete"
  root_shell "cp '$BUNDLE_ROOT'/scripts/*.sh '$INSTALL_ROOT/scripts/'"
  root_shell "chmod 755 '$INSTALL_ROOT/scripts/'*.sh"
  root_shell "chown -R '$DEPLOY_OWNER:$DEPLOY_GROUP' '$INSTALL_ROOT/compose' '$INSTALL_ROOT/scripts' '$INSTALL_ROOT/env' '$STATE_DIR' '$BACKUP_DIR_ROOT'"
}

install_or_enable_systemd() {
  root_shell "systemctl daemon-reload && systemctl enable '$SYSTEMD_UNIT_NAME'"
}

restart_systemd_service() {
  root_shell "systemctl restart '$SYSTEMD_UNIT_NAME'"
}

systemd_is_active() {
  root_shell "systemctl is-active '$SYSTEMD_UNIT_NAME'" >/dev/null 2>&1
}

list_legacy_containers() {
  docker ps -a --format '{{.Names}}' | grep -E "$LEGACY_CONTAINER_PATTERN" || true
}

list_legacy_networks() {
  docker network ls --format '{{.Name}}' | grep -E "$LEGACY_NETWORK_PATTERN" || true
}

list_port_80_containers() {
  docker ps --format '{{.Names}}\t{{.Ports}}' | awk -F '\t' '$2 ~ /0\.0\.0\.0:80->|\[::\]:80->/ {print $1}' || true
}

reload_or_start_systemd_service() {
  if systemd_is_active; then
    root_shell "systemctl reload '$SYSTEMD_UNIT_NAME'"
    return
  fi

  root_shell "systemctl start '$SYSTEMD_UNIT_NAME'"
}

systemd_unit_exists() {
  root_shell "systemctl cat '$SYSTEMD_UNIT_NAME' >/dev/null 2>&1"
}

systemd_state() {
  root_shell "systemctl is-active '$SYSTEMD_UNIT_NAME' 2>/dev/null || true"
}
