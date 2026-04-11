#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd -P)"
RELEASE_ID="${1:-${RELEASE_ID:-$(date -u +"%Y%m%d-%H%M%S")}}"
RELEASE_DIR="${ROOT}/.release/${RELEASE_ID}"
LOG_FILE="${RELEASE_DIR}/prepare-images.log"
BASE_IMAGES=(
  "mysql:8.2"
  "maven:3.9.9-eclipse-temurin-21"
  "eclipse-temurin:21-jre"
  "node:20-bullseye"
  "nginx:1.27-alpine"
)

mkdir -p "$RELEASE_DIR"
export PATH="$HOME/.npm-global/bin:/usr/local/bin:/usr/bin:/bin:$PATH"

{
  echo "[prepare-images] repo=$ROOT"
  echo "[prepare-images] release_id=$RELEASE_ID"
  date -u +"[prepare-images] started_at=%Y-%m-%dT%H:%M:%SZ"

  for image in "${BASE_IMAGES[@]}"; do
    echo "[prepare-images] pulling=$image"
    docker pull "$image"
  done

  docker compose -f "$ROOT/docker-compose.yml" --env-file "$ROOT/.env" build loom-server loom-web loom-node
  docker image inspect loom-loom-server >/dev/null
  docker image inspect loom-loom-web >/dev/null
  docker image inspect loom-loom-node >/dev/null

  date -u +"[prepare-images] finished_at=%Y-%m-%dT%H:%M:%SZ"
} 2>&1 | tee "$LOG_FILE"
