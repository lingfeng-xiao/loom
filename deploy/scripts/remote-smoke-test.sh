#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=remote-common.sh
source "$SCRIPT_DIR/remote-common.sh"

TARGET_ENV_FILE="${ENV_FILE:-}"
[[ -n "$TARGET_ENV_FILE" && -f "$TARGET_ENV_FILE" ]] && parse_env_file "$TARGET_ENV_FILE"

WEB_URL="${WEB_URL:-http://127.0.0.1:${LOOM_PUBLIC_PORT:-80}}"
API_URL="${API_URL:-${WEB_URL%/}/api}"
SMOKE_ATTEMPTS="${SMOKE_ATTEMPTS:-30}"
SMOKE_DELAY_SECONDS="${SMOKE_DELAY_SECONDS:-2}"
SMOKE_BODY_LIMIT="${SMOKE_BODY_LIMIT:-1200}"

LAST_HEADERS_FILE=""
LAST_BODY_FILE=""
LAST_HTTP_CODE=""
LAST_CURL_EXIT_CODE=0

cleanup() {
  [[ -n "$LAST_HEADERS_FILE" ]] && rm -f "$LAST_HEADERS_FILE"
  [[ -n "$LAST_BODY_FILE" ]] && rm -f "$LAST_BODY_FILE"
}

trap cleanup EXIT

request_once() {
  local url="$1"

  [[ -n "$LAST_HEADERS_FILE" ]] && rm -f "$LAST_HEADERS_FILE"
  [[ -n "$LAST_BODY_FILE" ]] && rm -f "$LAST_BODY_FILE"

  LAST_HEADERS_FILE="$(mktemp)"
  LAST_BODY_FILE="$(mktemp)"
  LAST_CURL_EXIT_CODE=0
  LAST_HTTP_CODE="$(curl -sS -D "$LAST_HEADERS_FILE" -o "$LAST_BODY_FILE" -w '%{http_code}' "$url")" || LAST_CURL_EXIT_CODE=$?

  [[ "$LAST_CURL_EXIT_CODE" -eq 0 && "$LAST_HTTP_CODE" =~ ^2[0-9][0-9]$ ]]
}

print_failure_context() {
  local url="$1"

  log "Smoke failed for $url"
  log "Last curl exit code: ${LAST_CURL_EXIT_CODE}"
  log "Last HTTP status: ${LAST_HTTP_CODE:-n/a}"

  if [[ -s "$LAST_HEADERS_FILE" ]]; then
    log "Response headers:"
    sed -n '1,20p' "$LAST_HEADERS_FILE" >&2
  fi

  if [[ -s "$LAST_BODY_FILE" ]]; then
    log "Response body:"
    head -c "$SMOKE_BODY_LIMIT" "$LAST_BODY_FILE" >&2
    printf '\n' >&2
  fi

  log "Container status:"
  docker ps -a --format 'table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}' >&2 || true

  for container in loom-loom-edge-1 loom-loom-web-1 loom-loom-server-1 loom-loom-node-1; do
    if docker ps -a --format '{{.Names}}' | grep -qx "$container"; then
      log "Recent logs for $container:"
      docker logs --tail 40 "$container" >&2 || true
    fi
  done
}

retry_request() {
  local url="$1"
  local attempt

  for ((attempt = 1; attempt <= SMOKE_ATTEMPTS; attempt++)); do
    if request_once "$url"; then
      return 0
    fi

    log "Smoke attempt $attempt/$SMOKE_ATTEMPTS failed for $url"
    sleep "$SMOKE_DELAY_SECONDS"
  done

  print_failure_context "$url"
  return 1
}

retry_request "$WEB_URL"
retry_request "${API_URL%/}/health"
retry_request "${API_URL%/}/nodes"

echo "smoke-test-passed"
