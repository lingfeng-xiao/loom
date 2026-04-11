#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd -P)"
RELEASE_ID="${1:-$(date -u +"%Y%m%d-%H%M%S")}"
RELEASE_DIR="${ROOT}/.release/${RELEASE_ID}"
STATUS="FAILED"
RELEASE_MODE="server-build"

mkdir -p "$RELEASE_DIR"
STARTED_AT="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
export RELEASE_ID STARTED_AT VALIDATED=false SUDO_READY=false PROXY_READY=false IMAGES_READY=false DEPLOYED=false HEALTHCHECK_PASSED=false RELEASE_MODE

if bash ./deploy/scripts/server-validate.sh "$RELEASE_ID"; then
  VALIDATED=true
  SUDO_READY=true
  PROXY_READY=true
  export VALIDATED SUDO_READY PROXY_READY
else
  bash ./deploy/scripts/server-release-report.sh "$RELEASE_ID" "$STATUS"
  exit 1
fi

if bash ./deploy/scripts/server-prepare-images.sh "$RELEASE_ID"; then
  IMAGES_READY=true
  export IMAGES_READY
else
  bash ./deploy/scripts/server-release-report.sh "$RELEASE_ID" "$STATUS"
  exit 1
fi

if bash ./deploy/scripts/server-deploy.sh "$RELEASE_ID"; then
  DEPLOYED=true
  export DEPLOYED
else
  bash ./deploy/scripts/server-release-report.sh "$RELEASE_ID" "$STATUS"
  exit 1
fi

if bash ./deploy/scripts/server-healthcheck.sh "$RELEASE_ID"; then
  HEALTHCHECK_PASSED=true
  STATUS="SUCCESS"
  export HEALTHCHECK_PASSED
fi

./deploy/scripts/server-release-report.sh "$RELEASE_ID" "$STATUS"
[[ "$STATUS" == "SUCCESS" ]]
