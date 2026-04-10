#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd -P)"
UNIT_SOURCE="$ROOT/deploy/systemd/loom.service"
UNIT_DEST="/etc/systemd/system/loom.service"
LOG_DIR="$ROOT/.tmp"
LOG_FILE="$LOG_DIR/install-loom-service.log"
EXPECTED_COMPOSE_PATH="/home/lingfeng/loom/docker-compose.yml"
EXPECTED_ENV_PATH="/home/lingfeng/loom/.env"

mkdir -p "$LOG_DIR"
export PATH="$HOME/.npm-global/bin:/usr/local/bin:/usr/bin:/bin:$PATH"

fail() {
  echo "[service] ERROR: $*" >&2
  exit 1
}

run_sudo() {
  if ! command -v sudo >/dev/null 2>&1; then
    fail "sudo is required"
  fi

  if sudo -n true >/dev/null 2>&1; then
    sudo "$@"
    return
  fi

  if [[ -n "${SUDO_PASSWORD:-}" ]]; then
    printf '%s\n' "$SUDO_PASSWORD" | sudo -S -p '' "$@"
    return
  fi

  fail "sudo access is required to install loom.service"
}

{
  echo "[service] repo=$ROOT"
  echo "[service] unit_source=$UNIT_SOURCE"
  echo "[service] unit_dest=$UNIT_DEST"
  date -u +"[service] started_at=%Y-%m-%dT%H:%M:%SZ"

  command -v systemctl >/dev/null 2>&1 || fail "systemctl is required"
  run_sudo install -m 0644 "$UNIT_SOURCE" "$UNIT_DEST"
  run_sudo systemctl daemon-reload

  unit_output="$(systemctl cat loom.service)"
  printf '%s\n' "$unit_output"

  grep -Fq "$EXPECTED_COMPOSE_PATH" <<<"$unit_output" || fail "loom.service does not reference $EXPECTED_COMPOSE_PATH"
  grep -Fq "$EXPECTED_ENV_PATH" <<<"$unit_output" || fail "loom.service does not reference $EXPECTED_ENV_PATH"
  if grep -Fq "/opt/loom" <<<"$unit_output"; then
    fail "loom.service still references /opt/loom"
  fi

  echo "[service] loom_service=ok"
  date -u +"[service] finished_at=%Y-%m-%dT%H:%M:%SZ"
} 2>&1 | tee "$LOG_FILE"
