#!/usr/bin/env bash
set -euo pipefail

WEB_URL="${WEB_URL:-http://127.0.0.1}"
API_URL="${API_URL:-${WEB_URL}/api}"

curl -fsS "$WEB_URL" >/dev/null
curl -fsS "$API_URL/health" >/dev/null
curl -fsS "$API_URL/nodes" >/dev/null

echo "Smoke test passed"
