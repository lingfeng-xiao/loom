#!/usr/bin/env bash
set -euo pipefail

REMOTE_ROOT="${REMOTE_ROOT:-~/loom}"

ssh jd "cd ${REMOTE_ROOT} && docker compose --env-file .env pull loom-mysql || true && docker compose --env-file .env up -d --build --remove-orphans"
