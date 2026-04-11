#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: ./run-delegation-session.sh --manifest <session-manifest.json> [--session-root <path>] [--dry-run]"
}

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
manifest=""
session_root=""
dry_run=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --manifest) manifest="$2"; shift 2 ;;
    --session-root) session_root="$2"; shift 2 ;;
    --dry-run) dry_run=1; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; usage; exit 1 ;;
  esac
done

if [[ -z "$manifest" ]]; then
  usage
  exit 1
fi

args=(--manifest "$manifest")
if [[ -n "$session_root" ]]; then
  args+=(--session-root "$session_root")
fi
if [[ "$dry_run" -eq 1 ]]; then
  args+=(--dry-run)
fi

exec python3 "$script_dir/delegation_session_runner.py" "${args[@]}"
