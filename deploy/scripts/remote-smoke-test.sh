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

curl -fsS "$WEB_URL" >/dev/null
curl -fsS "${API_URL%/}/health" >/dev/null
curl -fsS "${API_URL%/}/nodes" >/dev/null

echo "smoke-test-passed"
