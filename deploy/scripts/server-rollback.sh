#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd -P)"
TARGET_REF="${1:-HEAD~1}"
export PATH="$HOME/.npm-global/bin:/usr/local/bin:/usr/bin:/bin:$PATH"
ROLLBACK_ID="rollback-$(date -u +"%Y%m%d-%H%M%S")"
ROLLBACK_DIR="$ROOT/.release/$ROLLBACK_ID"
ROLLBACK_LOG="$ROLLBACK_DIR/rollback.log"
STARTED_AT="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
CURRENT_HEAD="$(git -C "$ROOT" rev-parse HEAD)"
RESOLVED_TARGET="$(git -C "$ROOT" rev-parse "$TARGET_REF")"

mkdir -p "$ROLLBACK_DIR"
export RELEASE_ID="$ROLLBACK_ID" STARTED_AT VALIDATED=false DEPLOYED=false HEALTHCHECK_PASSED=false

{
  echo "[rollback] repo=$ROOT"
  echo "[rollback] requested_ref=$TARGET_REF"
  echo "[rollback] resolved_target=$RESOLVED_TARGET"
  echo "[rollback] current_head=$CURRENT_HEAD"
  date -u +"[rollback] started_at=%Y-%m-%dT%H:%M:%SZ"
  git -C "$ROOT" reset --hard "$RESOLVED_TARGET"
  date -u +"[rollback] reset_finished_at=%Y-%m-%dT%H:%M:%SZ"
} 2>&1 | tee "$ROLLBACK_LOG"

STATUS="FAILED"
if ./deploy/scripts/server-deploy.sh "$ROLLBACK_ID"; then
  DEPLOYED=true
  export DEPLOYED
fi

if [[ "${DEPLOYED:-false}" == "true" ]] && ./deploy/scripts/server-healthcheck.sh "$ROLLBACK_ID"; then
  HEALTHCHECK_PASSED=true
  export HEALTHCHECK_PASSED
  STATUS="ROLLED_BACK"
fi

./deploy/scripts/server-release-report.sh "$ROLLBACK_ID" "$STATUS"

cat > "$ROLLBACK_DIR/rollback.md" <<EOF
# Rollback Report

- Rollback id: \`$ROLLBACK_ID\`
- Requested ref: \`$TARGET_REF\`
- Resolved target: \`$RESOLVED_TARGET\`
- Previous head: \`$CURRENT_HEAD\`
- Current head: \`$(git -C "$ROOT" rev-parse HEAD)\`
- Deploy succeeded: \`${DEPLOYED:-false}\`
- Healthcheck passed: \`${HEALTHCHECK_PASSED:-false}\`
- Status: \`$STATUS\`
EOF

[[ "$STATUS" == "ROLLED_BACK" ]]
