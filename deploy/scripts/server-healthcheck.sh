#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd -P)"
RELEASE_ID="${1:-${RELEASE_ID:-$(date -u +"%Y%m%d-%H%M%S")}}"
RELEASE_DIR="${ROOT}/.release/${RELEASE_ID}"
LOG_FILE="${RELEASE_DIR}/healthcheck.log"

mkdir -p "$RELEASE_DIR"
export PATH="$HOME/.npm-global/bin:/usr/local/bin:/usr/bin:/bin:$PATH"

SERVER_PORT="${LOOM_SERVER_PORT:-8080}"
PUBLIC_PORT="${LOOM_PUBLIC_PORT:-80}"
MAX_ATTEMPTS="${LOOM_HEALTHCHECK_ATTEMPTS:-30}"
SLEEP_SECONDS="${LOOM_HEALTHCHECK_SLEEP_SECONDS:-2}"

wait_for_url() {
  local name="$1"
  local url="$2"
  local attempt=1

  while (( attempt <= MAX_ATTEMPTS )); do
    if curl -fsS "$url" >/dev/null; then
      echo "[healthcheck] ${name}=ok attempt=${attempt}"
      return 0
    fi

    echo "[healthcheck] ${name}=retry attempt=${attempt}/${MAX_ATTEMPTS}"
    sleep "$SLEEP_SECONDS"
    attempt=$((attempt + 1))
  done

  echo "[healthcheck] ${name}=failed"
  return 1
}

{
  echo "[healthcheck] repo=$ROOT"
  echo "[healthcheck] release_id=$RELEASE_ID"
  date -u +"[healthcheck] started_at=%Y-%m-%dT%H:%M:%SZ"
  echo "[healthcheck] server_port=$SERVER_PORT"
  echo "[healthcheck] public_port=$PUBLIC_PORT"
  echo "[healthcheck] attempts=$MAX_ATTEMPTS"
  echo "[healthcheck] sleep_seconds=$SLEEP_SECONDS"
  docker compose ps
  echo
  docker ps --format '{{.Names}}\t{{.Image}}\t{{.Status}}'
  echo
  wait_for_url "server_health" "http://127.0.0.1:${SERVER_PORT}/actuator/health"
  echo
  curl -fsS "http://127.0.0.1:${SERVER_PORT}/actuator/health"
  echo
  wait_for_url "public_root" "http://127.0.0.1:${PUBLIC_PORT}/"
  echo
  curl -fsS "http://127.0.0.1:${PUBLIC_PORT}/"
  echo
  date -u +"[healthcheck] finished_at=%Y-%m-%dT%H:%M:%SZ"
} 2>&1 | tee "$LOG_FILE"
