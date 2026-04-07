#!/usr/bin/env bash
set -euo pipefail

REMOTE_NAME="${REMOTE_NAME:-origin}"
GITHUB_OWNER="${GITHUB_OWNER:-${GITHUB_REPOSITORY_OWNER:-}}"
ALLOW_TAGS="${ALLOW_TAGS:-0}"
FORBIDDEN_PACKAGE_NAMES="${FORBIDDEN_PACKAGE_NAMES:-template-api sprite-api sprite-web loom-server loom-web loom-node your-image}"

log() {
  printf '[release-preflight] %s\n' "$*"
}

die() {
  log "ERROR: $*" >&2
  exit 1
}

refs="$(git ls-remote --heads --tags "$REMOTE_NAME")"
ref_names="$(printf '%s\n' "$refs" | awk '{print $2}')"

unexpected_heads="$(printf '%s\n' "$ref_names" | grep '^refs/heads/' | grep -v '^refs/heads/main$' || true)"
if [[ -n "$unexpected_heads" ]]; then
  printf '%s\n' "$unexpected_heads" >&2
  die "Unexpected remote branches are still present"
fi

if [[ "$ALLOW_TAGS" != "1" ]]; then
  unexpected_tags="$(printf '%s\n' "$ref_names" | grep '^refs/tags/' | grep -v '\^{}$' || true)"
  if [[ -n "$unexpected_tags" ]]; then
    printf '%s\n' "$unexpected_tags" >&2
    die "Unexpected remote tags are still present"
  fi
fi

if [[ "${CI:-}" != "true" ]]; then
  push_url="$(git remote get-url --push "$REMOTE_NAME")"
  if [[ "$push_url" != ssh://git@ssh.github.com:443/* ]]; then
    die "Push remote for $REMOTE_NAME must use ssh.github.com over port 443"
  fi
fi

if [[ -z "$GITHUB_OWNER" ]]; then
  log "Skipping GHCR package checks because GITHUB_OWNER is not available"
  exit 0
fi

found_forbidden_packages=""
for package_name in $FORBIDDEN_PACKAGE_NAMES; do
  status_code="$(
    curl -o /dev/null -sSL -w '%{http_code}' \
      -H 'User-Agent: codex' \
      "https://github.com/users/${GITHUB_OWNER}/packages/container/${package_name}"
  )"

  if [[ "$status_code" != "404" ]]; then
    found_forbidden_packages+="${package_name} (status ${status_code})"$'\n'
  fi
done

if [[ -n "$found_forbidden_packages" ]]; then
  printf '%s' "$found_forbidden_packages" >&2
  die "Forbidden GHCR package pages are still reachable"
fi

log "Release preflight passed"
