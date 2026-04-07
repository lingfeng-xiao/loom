#!/usr/bin/env bash
set -euo pipefail

REMOTE_NAME="${REMOTE_NAME:-origin}"
GITHUB_OWNER="${GITHUB_OWNER:-${GITHUB_REPOSITORY_OWNER:-}}"
PACKAGE_TOKEN="${GHCR_TOKEN:-${GITHUB_TOKEN:-}}"
ALLOW_TAGS="${ALLOW_TAGS:-0}"
STRICT_GHCR_ALLOWLIST="${STRICT_GHCR_ALLOWLIST:-1}"

readonly KEEP_PACKAGES_REGEX='^(loom-server|loom-web|loom-node)$'

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
  log "Skipping GHCR package check because GITHUB_OWNER is not available"
  log "Release preflight passed"
  exit 0
fi

fetch_packages() {
  local url="$1"
  shift || true

  curl -fsSL \
    -H 'Accept: application/vnd.github+json' \
    -H 'User-Agent: codex' \
    "$@" \
    "$url"
}

packages_json="$(
  fetch_packages "https://api.github.com/users/${GITHUB_OWNER}/packages?package_type=container&per_page=100" || \
  {
    [[ -n "$PACKAGE_TOKEN" ]] || exit 1
    fetch_packages "https://api.github.com/users/${GITHUB_OWNER}/packages?package_type=container&per_page=100" \
      -H "Authorization: Bearer $PACKAGE_TOKEN" || \
    fetch_packages "https://api.github.com/user/packages?package_type=container&per_page=100" \
      -H "Authorization: Bearer $PACKAGE_TOKEN"
  }
)"

package_names="$(
  python3 -c 'import json, sys; print("\n".join(pkg["name"] for pkg in json.load(sys.stdin) if "name" in pkg))' <<<"$packages_json"
)"

if [[ "$STRICT_GHCR_ALLOWLIST" == "1" ]]; then
  unexpected_packages="$(printf '%s\n' "$package_names" | sed '/^$/d' | grep -vE "$KEEP_PACKAGES_REGEX" || true)"
  if [[ -n "$unexpected_packages" ]]; then
    printf '%s\n' "$unexpected_packages" >&2
    die "Unexpected GHCR packages are still present"
  fi
fi

log "Release preflight passed"
