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

  : "${MYSQL_DATABASE:=loom}"
  : "${MYSQL_USER:=loom}"
  : "${MYSQL_PASSWORD:=loom}"
  : "${MYSQL_ROOT_PASSWORD:=loom-root}"
  : "${LOOM_NODE_NAME:=loom-node}"
  : "${LOOM_NODE_TYPE:=server}"
  : "${LOOM_NODE_HOST:=$(hostname)}"
  : "${LOOM_SERVER_TOKEN:=change-me}"
  : "${LOOM_BOOTSTRAP_ENABLED:=true}"
  : "${LOOM_NODE_HEARTBEAT_INTERVAL_MS:=30000}"
  : "${LOOM_NODE_HEARTBEAT_INITIAL_DELAY_MS:=5000}"
  : "${LOOM_AI_PROVIDER_LABEL:=GitHub Models}"
  : "${LOOM_AI_BASE_URL:=https://models.github.ai/inference}"
  : "${LOOM_AI_MODEL:=openai/gpt-4.1-mini}"
  : "${LOOM_AI_TEMPERATURE:=0.2}"
  : "${LOOM_AI_API_KEY:=}"

  [[ -n "${LOOM_MODELS_API_KEY:-}" ]] && LOOM_AI_API_KEY="$LOOM_MODELS_API_KEY"

  : "${LOOM_SERVER_IMAGE:?set LOOM_SERVER_IMAGE}"
  : "${LOOM_WEB_IMAGE:?set LOOM_WEB_IMAGE}"
  : "${LOOM_NODE_IMAGE:?set LOOM_NODE_IMAGE}"

  {
    printf 'MYSQL_DATABASE=%s\n' "$MYSQL_DATABASE"
    printf 'MYSQL_USER=%s\n' "$MYSQL_USER"
    printf 'MYSQL_PASSWORD=%s\n' "$MYSQL_PASSWORD"
    printf 'MYSQL_ROOT_PASSWORD=%s\n' "$MYSQL_ROOT_PASSWORD"
    printf 'LOOM_PUBLIC_PORT=%s\n' "80"
    printf 'LOOM_NODE_NAME=%s\n' "$LOOM_NODE_NAME"
    printf 'LOOM_NODE_TYPE=%s\n' "$LOOM_NODE_TYPE"
    printf 'LOOM_NODE_HOST=%s\n' "$LOOM_NODE_HOST"
    printf 'LOOM_SERVER_TOKEN=%s\n' "$LOOM_SERVER_TOKEN"
    printf 'LOOM_BOOTSTRAP_ENABLED=%s\n' "$LOOM_BOOTSTRAP_ENABLED"
    printf 'LOOM_NODE_HEARTBEAT_INTERVAL_MS=%s\n' "$LOOM_NODE_HEARTBEAT_INTERVAL_MS"
    printf 'LOOM_NODE_HEARTBEAT_INITIAL_DELAY_MS=%s\n' "$LOOM_NODE_HEARTBEAT_INITIAL_DELAY_MS"
    printf 'LOOM_AI_PROVIDER_LABEL=%s\n' "$LOOM_AI_PROVIDER_LABEL"
    printf 'LOOM_AI_BASE_URL=%s\n' "$LOOM_AI_BASE_URL"
    printf 'LOOM_AI_MODEL=%s\n' "$LOOM_AI_MODEL"
    printf 'LOOM_AI_TEMPERATURE=%s\n' "$LOOM_AI_TEMPERATURE"
    printf 'LOOM_AI_API_KEY=%s\n' "$LOOM_AI_API_KEY"
    printf 'LOOM_VAULT_HOST_DIR=%s\n' "$INSTALL_ROOT/data/vault"
    printf 'LOOM_SERVER_LOG_HOST_DIR=%s\n' "$INSTALL_ROOT/data/logs/server"
    printf 'LOOM_NODE_LOG_HOST_DIR=%s\n' "$INSTALL_ROOT/data/logs/node"
    printf 'LOOM_NODE_STATE_HOST_DIR=%s\n' "$INSTALL_ROOT/data/node-state"
    printf 'LOOM_SERVER_IMAGE=%s\n' "$LOOM_SERVER_IMAGE"
    printf 'LOOM_WEB_IMAGE=%s\n' "$LOOM_WEB_IMAGE"
    printf 'LOOM_NODE_IMAGE=%s\n' "$LOOM_NODE_IMAGE"
  } > "$tmp_env"

  root_shell "install -D -m 600 -o '$DEPLOY_OWNER' -g '$DEPLOY_GROUP' '$tmp_env' '$CANDIDATE_ENV_FILE'"
  rm -f "$tmp_env"
}

migrate_existing_data() {
  if root_shell "[ ! -d '$DEPLOY_STAGE_ROOT/deploy/data' ]"; then
    return
  fi

  root_shell "if [ -d '$DEPLOY_STAGE_ROOT/deploy/data/vault' ] && [ -z \"\$(ls -A '$INSTALL_ROOT/data/vault' 2>/dev/null)\" ]; then cp -a '$DEPLOY_STAGE_ROOT/deploy/data/vault/.' '$INSTALL_ROOT/data/vault/'; fi"
  root_shell "if [ -d '$DEPLOY_STAGE_ROOT/deploy/data/logs/server' ] && [ -z \"\$(ls -A '$INSTALL_ROOT/data/logs/server' 2>/dev/null)\" ]; then cp -a '$DEPLOY_STAGE_ROOT/deploy/data/logs/server/.' '$INSTALL_ROOT/data/logs/server/'; fi"
  root_shell "if [ -d '$DEPLOY_STAGE_ROOT/deploy/data/logs/node' ] && [ -z \"\$(ls -A '$INSTALL_ROOT/data/logs/node' 2>/dev/null)\" ]; then cp -a '$DEPLOY_STAGE_ROOT/deploy/data/logs/node/.' '$INSTALL_ROOT/data/logs/node/'; fi"
  root_shell "if [ -d '$DEPLOY_STAGE_ROOT/deploy/data/node-state' ] && [ -z \"\$(ls -A '$INSTALL_ROOT/data/node-state' 2>/dev/null)\" ]; then cp -a '$DEPLOY_STAGE_ROOT/deploy/data/node-state/.' '$INSTALL_ROOT/data/node-state/'; fi"
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
  migrate_existing_data

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
