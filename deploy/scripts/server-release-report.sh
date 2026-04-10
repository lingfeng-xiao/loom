#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd -P)"
RELEASE_ID="${1:?release id required}"
STATUS="${2:?status required}"
RELEASE_DIR="${ROOT}/.release/${RELEASE_ID}"
RESULT_FILE="${RELEASE_DIR}/release.json"
ROLLBACK_FILE="${RELEASE_DIR}/rollback.md"
FINISHED_AT="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
REPO_HEAD="$(git -C "$ROOT" rev-parse HEAD)"
PREV_HEAD="$(git -C "$ROOT" rev-parse HEAD~1 2>/dev/null || true)"
ROLLBACK_READY=false
if [[ -n "$PREV_HEAD" ]]; then
  ROLLBACK_READY=true
fi

mkdir -p "$RELEASE_DIR"

cat > "$RESULT_FILE" <<EOF
{
  "release_id": "$RELEASE_ID",
  "started_at": "${STARTED_AT:-$FINISHED_AT}",
  "finished_at": "$FINISHED_AT",
  "operator": "${USER:-unknown}",
  "repo_head": "$REPO_HEAD",
  "release_mode": "${RELEASE_MODE:-server-build}",
  "validated": ${VALIDATED:-false},
  "sudo_ready": ${SUDO_READY:-false},
  "proxy_ready": ${PROXY_READY:-false},
  "images_ready": ${IMAGES_READY:-false},
  "deployed": ${DEPLOYED:-false},
  "healthcheck_passed": ${HEALTHCHECK_PASSED:-false},
  "rollback_ready": $ROLLBACK_READY,
  "status": "$STATUS"
}
EOF

cat > "$ROLLBACK_FILE" <<EOF
# Rollback

- Current release id: \`$RELEASE_ID\`
- Current head: \`$REPO_HEAD\`
- Suggested rollback ref: \`${PREV_HEAD:-HEAD~1}\`
- Rollback ready: \`$ROLLBACK_READY\`

Run:

\`\`\`bash
cd "$ROOT"
./deploy/scripts/server-rollback.sh ${PREV_HEAD:-HEAD~1}
\`\`\`
EOF
