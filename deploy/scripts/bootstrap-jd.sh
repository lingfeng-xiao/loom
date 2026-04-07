#!/usr/bin/env bash
set -euo pipefail

ROOT="${1:-$HOME/loom}"

mkdir -p \
  "$ROOT/deploy/data/vault" \
  "$ROOT/deploy/data/logs/server" \
  "$ROOT/deploy/data/logs/node" \
  "$ROOT/deploy/data/node-state"
if [[ -f "$ROOT/.env.example" && ! -f "$ROOT/.env" ]]; then
  cp "$ROOT/.env.example" "$ROOT/.env"
fi

echo "Prepared Loom directories under $ROOT"
echo "Next: clone or pull the repository there, then run deploy-jd.sh"
