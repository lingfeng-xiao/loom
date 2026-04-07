#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="${ENV_FILE:-}"

if [[ -n "$ENV_FILE" && -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

WEB_URL="${WEB_URL:-http://127.0.0.1:${LOOM_PUBLIC_PORT:-80}}"
API_URL="${API_URL:-${WEB_URL%/}/api}"

curl -fsS "$WEB_URL" >/dev/null
curl -fsS "${API_URL%/}/health" >/dev/null
curl -fsS "${API_URL%/}/nodes" >/dev/null

echo "smoke-test-passed"
