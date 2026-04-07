#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=remote-common.sh
source "$SCRIPT_DIR/remote-common.sh"

CANDIDATE_ENV_FILE="$STATE_DIR/candidate.env"
CURRENT_STAGE_ENV_FILE="$DEPLOY_STAGE_ROOT/.env"

render_candidate_env() {
  local source_env_file="$1"
  local tmp_env
  tmp_env="$(mktemp)"

  parse_env_file "$source_env_file"

  : "${MYSQL_DATABASE:=template}"
  : "${MYSQL_USER:=template}"
  : "${MYSQL_PASSWORD:=template}"
  : "${MYSQL_ROOT_PASSWORD:=template-root}"
  : "${TEMPLATE_PUBLIC_PORT:=80}"
  : "${TEMPLATE_SERVER_PORT:=8080}"
  : "${TEMPLATE_NODE_PORT:=8090}"
  : "${TEMPLATE_NODE_NAME:=template-node}"
  : "${TEMPLATE_NODE_TYPE:=server}"
  : "${TEMPLATE_NODE_HOST:=$(hostname)}"
  : "${TEMPLATE_SERVER_TOKEN:=change-me}"
  : "${TEMPLATE_NODE_HEARTBEAT_TIMEOUT_SECONDS:=90}"
  : "${TEMPLATE_NODE_HEARTBEAT_INTERVAL_MS:=30000}"
  : "${TEMPLATE_NODE_HEARTBEAT_INITIAL_DELAY_MS:=5000}"
  : "${TEMPLATE_NODE_STATE_HOST_DIR:=$INSTALL_ROOT/data/node-state}"

  : "${TEMPLATE_SERVER_IMAGE:?set TEMPLATE_SERVER_IMAGE}"
  : "${TEMPLATE_WEB_IMAGE:?set TEMPLATE_WEB_IMAGE}"
  : "${TEMPLATE_NODE_IMAGE:?set TEMPLATE_NODE_IMAGE}"

  {
    printf 'MYSQL_DATABASE=%s\n' "$MYSQL_DATABASE"
    printf 'MYSQL_USER=%s\n' "$MYSQL_USER"
    printf 'MYSQL_PASSWORD=%s\n' "$MYSQL_PASSWORD"
    printf 'MYSQL_ROOT_PASSWORD=%s\n' "$MYSQL_ROOT_PASSWORD"
    printf 'TEMPLATE_PUBLIC_PORT=%s\n' "$TEMPLATE_PUBLIC_PORT"
    printf 'TEMPLATE_SERVER_PORT=%s\n' "$TEMPLATE_SERVER_PORT"
    printf 'TEMPLATE_NODE_PORT=%s\n' "$TEMPLATE_NODE_PORT"
    printf 'TEMPLATE_NODE_NAME=%s\n' "$TEMPLATE_NODE_NAME"
    printf 'TEMPLATE_NODE_TYPE=%s\n' "$TEMPLATE_NODE_TYPE"
    printf 'TEMPLATE_NODE_HOST=%s\n' "$TEMPLATE_NODE_HOST"
    printf 'TEMPLATE_SERVER_TOKEN=%s\n' "$TEMPLATE_SERVER_TOKEN"
    printf 'TEMPLATE_NODE_HEARTBEAT_TIMEOUT_SECONDS=%s\n' "$TEMPLATE_NODE_HEARTBEAT_TIMEOUT_SECONDS"
    printf 'TEMPLATE_NODE_HEARTBEAT_INTERVAL_MS=%s\n' "$TEMPLATE_NODE_HEARTBEAT_INTERVAL_MS"
    printf 'TEMPLATE_NODE_HEARTBEAT_INITIAL_DELAY_MS=%s\n' "$TEMPLATE_NODE_HEARTBEAT_INITIAL_DELAY_MS"
    printf 'TEMPLATE_NODE_STATE_HOST_DIR=%s\n' "$TEMPLATE_NODE_STATE_HOST_DIR"
    printf 'TEMPLATE_SERVER_IMAGE=%s\n' "$TEMPLATE_SERVER_IMAGE"
    printf 'TEMPLATE_WEB_IMAGE=%s\n' "$TEMPLATE_WEB_IMAGE"
    printf 'TEMPLATE_NODE_IMAGE=%s\n' "$TEMPLATE_NODE_IMAGE"
  } > "$tmp_env"

  root_shell "install -D -m 600 -o '$DEPLOY_OWNER' -g '$DEPLOY_GROUP' '$tmp_env' '$CANDIDATE_ENV_FILE'"
  rm -f "$tmp_env"
}

commit_candidate_env() {
  root_shell "install -D -m 600 -o '$DEPLOY_OWNER' -g '$DEPLOY_GROUP' '$CANDIDATE_ENV_FILE' '$ENV_FILE'"
  root_shell "install -D -m 600 -o '$DEPLOY_OWNER' -g '$DEPLOY_GROUP' '$CANDIDATE_ENV_FILE' '$STATE_DIR/last_successful.env'"
}

main() {
  sync_bundle_to_install_root
  install_or_enable_systemd

  if root_shell "[ -f '$ENV_FILE' ]"; then
    root_shell "install -D -m 600 -o '$DEPLOY_OWNER' -g '$DEPLOY_GROUP' '$ENV_FILE' '$STATE_DIR/previous.env'"
    source_env_file="$ENV_FILE"
  else
    [[ -f "$CURRENT_STAGE_ENV_FILE" ]] || die "Missing seed env file: $CURRENT_STAGE_ENV_FILE"
    source_env_file="$CURRENT_STAGE_ENV_FILE"
  fi

  render_candidate_env "$source_env_file"

  "$INSTALL_ROOT/scripts/remote-backup-legacy.sh"
  "$INSTALL_ROOT/scripts/remote-retire-legacy.sh"

  ENV_FILE="$CANDIDATE_ENV_FILE" "$INSTALL_ROOT/scripts/remote-preflight.sh"

  if ! ENV_FILE="$CANDIDATE_ENV_FILE" "$INSTALL_ROOT/scripts/remote-deploy.sh"; then
    FAILED_ENV_FILE="$CANDIDATE_ENV_FILE" "$INSTALL_ROOT/scripts/remote-rollback.sh"
    exit 1
  fi

  if ! ENV_FILE="$CANDIDATE_ENV_FILE" "$INSTALL_ROOT/scripts/remote-smoke-test.sh"; then
    FAILED_ENV_FILE="$CANDIDATE_ENV_FILE" "$INSTALL_ROOT/scripts/remote-rollback.sh"
    exit 1
  fi

  commit_candidate_env
  reload_or_start_systemd_service
  ENV_FILE="$ENV_FILE" "$INSTALL_ROOT/scripts/remote-smoke-test.sh"
  log "Release completed successfully"
}

main "$@"
