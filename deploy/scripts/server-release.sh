#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd -P)"
RELEASE_ID="${1:-$(date -u +"%Y%m%d-%H%M%S")}"
RELEASE_DIR="${ROOT}/.release/${RELEASE_ID}"
STATUS="FAILED"

mkdir -p "$RELEASE_DIR"
STARTED_AT="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
export RELEASE_ID STARTED_AT VALIDATED=false DEPLOYED=false HEALTHCHECK_PASSED=false

if ./deploy/scripts/server-validate.sh "$RELEASE_ID"; then
  VALIDATED=true
  export VALIDATED
else
  ./deploy/scripts/server-release-report.sh "$RELEASE_ID" "$STATUS"
  exit 1
fi

if ./deploy/scripts/server-deploy.sh "$RELEASE_ID"; then
  DEPLOYED=true
  export DEPLOYED
else
  ./deploy/scripts/server-release-report.sh "$RELEASE_ID" "$STATUS"
  exit 1
fi

if ./deploy/scripts/server-healthcheck.sh "$RELEASE_ID"; then
  HEALTHCHECK_PASSED=true
  STATUS="SUCCESS"
  export HEALTHCHECK_PASSED
fi

./deploy/scripts/server-release-report.sh "$RELEASE_ID" "$STATUS"
[[ "$STATUS" == "SUCCESS" ]]
