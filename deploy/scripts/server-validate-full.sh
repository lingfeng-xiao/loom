#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd -P)"
RELEASE_ID="${1:-${RELEASE_ID:-$(date -u +"%Y%m%d-%H%M%S")}}"
RELEASE_DIR="${ROOT}/.release/${RELEASE_ID}"
LOG_FILE="${RELEASE_DIR}/validate-full.log"

mkdir -p "$RELEASE_DIR"
export PATH="$HOME/.npm-global/bin:/usr/local/bin:/usr/bin:/bin:$PATH"

if [[ -d "$HOME/jdk-21.0.2" && -z "${JAVA_HOME:-}" ]]; then
  export JAVA_HOME="$HOME/jdk-21.0.2"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

resolve_app_dir() {
  local preferred="$1"
  local legacy="$2"

  if [[ -d "$ROOT/$preferred" ]]; then
    printf '%s\n' "$preferred"
    return 0
  fi

  if [[ -d "$ROOT/$legacy" ]]; then
    printf '%s\n' "$legacy"
    return 0
  fi

  echo "Missing application directory: $preferred or $legacy" >&2
  return 1
}

prepare_maven_wrapper() {
  local app_dir="$1"
  local wrapper_path="$ROOT/$app_dir/.mvnw.codex"

  tr -d '\r' < "$ROOT/$app_dir/mvnw" > "$wrapper_path"
  chmod +x "$wrapper_path"
  printf '%s\n' "$wrapper_path"
}

SERVER_DIR="$(resolve_app_dir "apps/server" "apps/loom-server")"
NODE_DIR="$(resolve_app_dir "apps/node" "apps/loom-node")"
WEB_DIR="$(resolve_app_dir "apps/web" "apps/loom-web")"
SERVER_MVNW="$(prepare_maven_wrapper "$SERVER_DIR")"
NODE_MVNW="$(prepare_maven_wrapper "$NODE_DIR")"
trap 'rm -f "$SERVER_MVNW" "$NODE_MVNW"' EXIT

{
  echo "[validate-full] repo=$ROOT"
  echo "[validate-full] release_id=$RELEASE_ID"
  echo "[validate-full] server_dir=$SERVER_DIR"
  echo "[validate-full] node_dir=$NODE_DIR"
  echo "[validate-full] web_dir=$WEB_DIR"
  date -u +"[validate-full] started_at=%Y-%m-%dT%H:%M:%SZ"
  npx -p typescript@5.6.3 tsc -p packages/contracts/tsconfig.json --noEmit
  (cd "$SERVER_DIR" && "$SERVER_MVNW" -q test && "$SERVER_MVNW" -q -DskipTests package)
  (cd "$NODE_DIR" && "$NODE_MVNW" -q test)
  (cd "$WEB_DIR" && npm ci && npm run build)
  docker compose -f "$ROOT/docker-compose.yml" --env-file "$ROOT/.env" config >/dev/null
  date -u +"[validate-full] finished_at=%Y-%m-%dT%H:%M:%SZ"
} 2>&1 | tee "$LOG_FILE"
