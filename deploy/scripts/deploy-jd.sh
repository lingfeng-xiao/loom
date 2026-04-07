#!/usr/bin/env bash
set -euo pipefail

ROOT="${1:-$HOME/loom}"

cd "$ROOT"
docker compose --env-file .env config >/dev/null
docker compose --env-file .env pull loom-mysql || true
docker compose --env-file .env up -d --build --remove-orphans
