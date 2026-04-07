#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=remote-common.sh
source "$SCRIPT_DIR/remote-common.sh"

ROLLBACK_ENV_FILE="${ROLLBACK_ENV_FILE:-$STATE_DIR/last_successful.env}"
FAILED_ENV_FILE="${FAILED_ENV_FILE:-}"

if [[ -f "$ROLLBACK_ENV_FILE" ]]; then
  compose_with_env "$ROLLBACK_ENV_FILE" up -d --remove-orphans
  root_shell "install -D -m 600 '$ROLLBACK_ENV_FILE' '$ENV_FILE'"
  if systemd_is_active; then
    restart_systemd_service
  fi
  log "Rolled back using $ROLLBACK_ENV_FILE"
  exit 0
fi

if [[ -n "$FAILED_ENV_FILE" && -f "$FAILED_ENV_FILE" ]]; then
  compose_with_env "$FAILED_ENV_FILE" down --remove-orphans || true
fi

die "Rollback failed because no successful release snapshot exists"
