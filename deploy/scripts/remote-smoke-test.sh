#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="${ENV_FILE:-}"

if [[ -n "$ENV_FILE" && -f "$ENV_FILE" ]]; then
  while IFS= read -r line || [[ -n "$line" ]]; do
    line="${line%$'\r'}"
    [[ -z "$line" || "$line" =~ ^[[:space:]]*# ]] && continue
    key="${line%%=*}"
    value="${line#*=}"
    export "${key}=${value}"
  done < "$ENV_FILE"
fi

WEB_URL="${WEB_URL:-http://127.0.0.1:${LOOM_PUBLIC_PORT:-80}}"
API_URL="${API_URL:-${WEB_URL%/}/api}"

retry_curl() {
  local url="$1"
  local attempts="${2:-30}"
  local delay_seconds="${3:-2}"
  local attempt

  for ((attempt = 1; attempt <= attempts; attempt++)); do
    if curl -fsS "$url" >/dev/null; then
      return 0
    fi
    sleep "$delay_seconds"
  done

  curl -fsS "$url" >/dev/null
}

retry_curl "$WEB_URL"
retry_curl "${API_URL%/}/health"
retry_curl "${API_URL%/}/nodes"

echo "smoke-test-passed"
