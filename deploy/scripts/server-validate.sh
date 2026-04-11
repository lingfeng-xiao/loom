#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd -P)"
RELEASE_ID="${1:-${RELEASE_ID:-$(date -u +"%Y%m%d-%H%M%S")}}"
RELEASE_DIR="${ROOT}/.release/${RELEASE_ID}"
LOG_FILE="${RELEASE_DIR}/validate.log"
EXPECTED_ROOT="/home/lingfeng/loom"
EXPECTED_BRANCH="main"
EXPECTED_COMPOSE_PATH="${EXPECTED_ROOT}/docker-compose.yml"
EXPECTED_ENV_PATH="${EXPECTED_ROOT}/.env"
EXPECTED_PROXY="http://127.0.0.1:7890/"

mkdir -p "$RELEASE_DIR"
export PATH="$HOME/.npm-global/bin:/usr/local/bin:/usr/bin:/bin:$PATH"

fail() {
  echo "[validate] ERROR: $*" >&2
  exit 1
}

{
  echo "[validate] repo=$ROOT"
  echo "[validate] release_id=$RELEASE_ID"
  date -u +"[validate] started_at=%Y-%m-%dT%H:%M:%SZ"

  [[ "$ROOT" == "$EXPECTED_ROOT" ]] || fail "expected repo root $EXPECTED_ROOT but found $ROOT"

  current_branch="$(git -C "$ROOT" branch --show-current)"
  echo "[validate] branch=$current_branch"
  [[ "$current_branch" == "$EXPECTED_BRANCH" ]] || fail "expected branch $EXPECTED_BRANCH but found $current_branch"

  current_head="$(git -C "$ROOT" rev-parse HEAD)"
  echo "[validate] head=$current_head"

  status_output="$(git -C "$ROOT" status --short)"
  if [[ -n "$status_output" ]]; then
    printf '%s\n' "$status_output"
    fail "git status is not clean"
  fi
  echo "[validate] git_status=clean"

  docker compose -f "$EXPECTED_COMPOSE_PATH" --env-file "$EXPECTED_ENV_PATH" config >/dev/null
  echo "[validate] compose_config=ok"

  command -v systemctl >/dev/null 2>&1 || fail "systemctl is required for validation"
  command -v sudo >/dev/null 2>&1 || fail "sudo is required for validation"
  sudo -n true >/dev/null 2>&1 || fail "passwordless sudo is required"
  echo "[validate] sudo_ready=ok"

  systemctl is-active --quiet mihomo.service || fail "mihomo.service is not active"
  echo "[validate] mihomo_service=active"

  ss -ltn | grep -Fq "127.0.0.1:7890" || fail "mihomo proxy is not listening on 127.0.0.1:7890"
  echo "[validate] proxy_listener=ok"

  curl -I --max-time 15 --proxy "$EXPECTED_PROXY" "https://auth.docker.io/token?service=registry.docker.io" >/dev/null
  echo "[validate] proxy_connectivity=ok"

  docker_info="$(docker info 2>/dev/null)"
  grep -Fq "HTTP Proxy: $EXPECTED_PROXY" <<<"$docker_info" || fail "docker daemon is missing HTTP proxy $EXPECTED_PROXY"
  grep -Fq "HTTPS Proxy: $EXPECTED_PROXY" <<<"$docker_info" || fail "docker daemon is missing HTTPS proxy $EXPECTED_PROXY"
  echo "[validate] docker_proxy=ok"

  unit_output="$(systemctl cat loom.service)"
  printf '%s\n' "$unit_output"

  grep -Fq "$EXPECTED_COMPOSE_PATH" <<<"$unit_output" || fail "loom.service does not reference $EXPECTED_COMPOSE_PATH"
  grep -Fq "$EXPECTED_ENV_PATH" <<<"$unit_output" || fail "loom.service does not reference $EXPECTED_ENV_PATH"
  if grep -Fq "/opt/loom" <<<"$unit_output"; then
    fail "loom.service still references /opt/loom"
  fi
  echo "[validate] loom_service=ok"

  date -u +"[validate] finished_at=%Y-%m-%dT%H:%M:%SZ"
} 2>&1 | tee "$LOG_FILE"
