#!/usr/bin/env bash
set -euo pipefail

DEPLOY_ROOT="${DEPLOY_ROOT:-$HOME/loom-deploy}"
COMPOSE_FILE="${COMPOSE_FILE:-$DEPLOY_ROOT/docker-compose.release.yml}"
ENV_FILE="${ENV_FILE:-$DEPLOY_ROOT/.env}"

mkdir -p "$DEPLOY_ROOT"

if [[ ! -f "$COMPOSE_FILE" ]]; then
  echo "Missing compose file: $COMPOSE_FILE" >&2
  exit 1
fi

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing env file: $ENV_FILE" >&2
  exit 1
fi

if [[ -n "${GHCR_USERNAME:-}" && -n "${GHCR_TOKEN:-}" ]]; then
  printf '%s' "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USERNAME" --password-stdin
fi

docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" pull loom-mysql || true
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" pull loom-server loom-web loom-node
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d --remove-orphans
