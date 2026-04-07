#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=remote-common.sh
source "$SCRIPT_DIR/remote-common.sh"

TARGET_ENV_FILE="${1:-$ENV_FILE}"

[[ -f "$COMPOSE_FILE" ]] || die "Missing compose file: $COMPOSE_FILE"
[[ -f "$TARGET_ENV_FILE" ]] || die "Missing env file: $TARGET_ENV_FILE"

if [[ -n "${GHCR_USERNAME:-}" && -n "${GHCR_TOKEN:-}" ]]; then
  printf '%s' "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USERNAME" --password-stdin
fi

compose_with_env "$TARGET_ENV_FILE" pull loom-mysql || true
compose_with_env "$TARGET_ENV_FILE" pull loom-edge loom-server loom-web loom-node
compose_with_env "$TARGET_ENV_FILE" up -d --remove-orphans
