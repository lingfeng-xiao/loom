#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd -P)"
RELEASE_ID="${1:?release id required}"
RELEASE_DIR="$ROOT/.release/$RELEASE_ID"
RESULT_FILE="$RELEASE_DIR/release.json"
LOG_FILE="$RELEASE_DIR/cleanup.log"
LEGACY_ROOT="/opt/loom"

export PATH="$HOME/.npm-global/bin:/usr/local/bin:/usr/bin:/bin:$PATH"

fail() {
  echo "[cleanup] ERROR: $*" >&2
  exit 1
}

run_sudo() {
  command -v sudo >/dev/null 2>&1 || fail "sudo is required for legacy cleanup"
  sudo -n "$@"
}

[[ -f "$RESULT_FILE" ]] || fail "release result not found: $RESULT_FILE"
grep -Fq '"status": "SUCCESS"' "$RESULT_FILE" || fail "release $RELEASE_ID is not marked SUCCESS"
grep -Fq '"validated": true' "$RESULT_FILE" || fail "release $RELEASE_ID did not finish validated=true"
grep -Fq '"deployed": true' "$RESULT_FILE" || fail "release $RELEASE_ID did not finish deployed=true"
grep -Fq '"healthcheck_passed": true' "$RESULT_FILE" || fail "release $RELEASE_ID did not finish healthcheck_passed=true"

{
  echo "[cleanup] repo=$ROOT"
  echo "[cleanup] release_id=$RELEASE_ID"
  echo "[cleanup] legacy_root=$LEGACY_ROOT"
  date -u +"[cleanup] started_at=%Y-%m-%dT%H:%M:%SZ"

  if [[ -e "$LEGACY_ROOT" ]]; then
    run_sudo true >/dev/null 2>&1 || fail "passwordless sudo is required for legacy cleanup"
    run_sudo rm -rf "$LEGACY_ROOT"
    echo "[cleanup] removed_legacy_root=true"
  else
    echo "[cleanup] removed_legacy_root=already-absent"
  fi

  date -u +"[cleanup] finished_at=%Y-%m-%dT%H:%M:%SZ"
} 2>&1 | tee "$LOG_FILE"
