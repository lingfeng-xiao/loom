#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd -P)"
RELEASE_ID="${1:-${RELEASE_ID:-$(date -u +"%Y%m%d-%H%M%S")}}"
RELEASE_DIR="${ROOT}/.release/${RELEASE_ID}"
LOG_FILE="${RELEASE_DIR}/deploy.log"

mkdir -p "$RELEASE_DIR"
export PATH="$HOME/.npm-global/bin:/usr/local/bin:/usr/bin:/bin:$PATH"

has_passwordless_sudo=0
if command -v sudo >/dev/null 2>&1 && sudo -n true >/dev/null 2>&1; then
  has_passwordless_sudo=1
fi

service_visible=0
if command -v systemctl >/dev/null 2>&1 && systemctl cat loom.service >/dev/null 2>&1; then
  service_visible=1
fi

{
  echo "[deploy] repo=$ROOT"
  echo "[deploy] release_id=$RELEASE_ID"
  date -u +"[deploy] started_at=%Y-%m-%dT%H:%M:%SZ"
  if [[ "$service_visible" -eq 1 && "$has_passwordless_sudo" -eq 1 ]]; then
    sudo systemctl daemon-reload
    if sudo systemctl is-active --quiet loom.service; then
      sudo systemctl reload loom.service
    else
      sudo systemctl start loom.service
    fi
  else
    if [[ "$service_visible" -eq 1 && "$has_passwordless_sudo" -ne 1 ]]; then
      echo "[deploy] loom.service is visible, but passwordless sudo is unavailable. Falling back to direct docker compose."
    fi
    docker compose -f "$ROOT/docker-compose.yml" --env-file "$ROOT/.env" up -d --build --remove-orphans
  fi
  date -u +"[deploy] finished_at=%Y-%m-%dT%H:%M:%SZ"
} 2>&1 | tee "$LOG_FILE"
