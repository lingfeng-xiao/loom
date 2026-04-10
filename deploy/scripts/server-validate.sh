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

  if ! command -v systemctl >/dev/null 2>&1; then
    fail "systemctl is required for loom.service validation"
  fi

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
