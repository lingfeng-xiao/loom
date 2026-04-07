#!/usr/bin/env bash
set -euo pipefail

SPRITE_ROOT="${1:-$HOME/worktrees/sprite/deploy/prod}"
BACKUP_DIR="${2:-$HOME/loom/retired-sprite}"

mkdir -p "$BACKUP_DIR"
docker ps --format 'table {{.Names}}\t{{.Image}}\t{{.Ports}}' > "$BACKUP_DIR/docker-ps-before-sprite-retire.txt" || true
ss -ltnp > "$BACKUP_DIR/ports-before-sprite-retire.txt" || true
docker volume ls > "$BACKUP_DIR/docker-volumes-before-sprite-retire.txt" || true
docker network ls > "$BACKUP_DIR/docker-networks-before-sprite-retire.txt" || true

if [[ -d "$SPRITE_ROOT" && -f "$SPRITE_ROOT/docker-compose.yml" ]]; then
  (
    cd "$SPRITE_ROOT"
    docker compose config > "$BACKUP_DIR/sprite-compose-config.txt" || true
    docker compose down --remove-orphans
  )
fi

docker rm -f sprite-app sprite-proxy sprite-web-1 sprite-mysql 2>/dev/null || true
docker ps --format 'table {{.Names}}\t{{.Image}}\t{{.Ports}}' > "$BACKUP_DIR/docker-ps-after-sprite-retire.txt" || true
ss -ltnp > "$BACKUP_DIR/ports-after-sprite-retire.txt" || true

echo "Retired sprite containers from $SPRITE_ROOT"
echo "Compose volumes and networks were intentionally retained for the acceptance window."
