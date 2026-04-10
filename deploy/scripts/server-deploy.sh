#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd -P)"
RELEASE_ID="${1:-${RELEASE_ID:-$(date -u +"%Y%m%d-%H%M%S")}}"
RELEASE_DIR="${ROOT}/.release/${RELEASE_ID}"
LOG_FILE="${RELEASE_DIR}/deploy.log"

mkdir -p "$RELEASE_DIR"
export PATH="$HOME/.npm-global/bin:/usr/local/bin:/usr/bin:/bin:$PATH"

fail() {
  echo "[deploy] ERROR: $*" >&2
  exit 1
}

run_sudo() {
  command -v sudo >/dev/null 2>&1 || fail "sudo is required for deploy"
  sudo -n "$@"
}

command -v systemctl >/dev/null 2>&1 || fail "systemctl is required for deploy"
run_sudo true >/dev/null 2>&1 || fail "passwordless sudo is required for deploy"
systemctl cat loom.service >/dev/null 2>&1 || fail "loom.service must be installed before deploy"

{
  echo "[deploy] repo=$ROOT"
  echo "[deploy] release_id=$RELEASE_ID"
  date -u +"[deploy] started_at=%Y-%m-%dT%H:%M:%SZ"
  run_sudo systemctl daemon-reload
  if run_sudo systemctl is-active --quiet loom.service; then
    run_sudo systemctl reload loom.service
  else
    run_sudo systemctl start loom.service
  fi
  date -u +"[deploy] finished_at=%Y-%m-%dT%H:%M:%SZ"
} 2>&1 | tee "$LOG_FILE"
