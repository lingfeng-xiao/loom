# Loom Rollback

## Primary Rollback Path

The standard rollback entrypoint is:

```bash
/opt/loom/scripts/remote-rollback.sh
```

It restores the stack from:

- `/opt/loom/state/last_successful.env`

and then re-runs:

```bash
docker compose -f /opt/loom/compose/docker-compose.production.yml --env-file /opt/loom/env/.env.production up -d --remove-orphans
```

## When To Roll Back

Roll back immediately if:

- deploy succeeds but smoke fails
- `/` returns 5xx
- `/api/health` or `/api/nodes` returns non-200 after the readiness window
- real chat generation fails because of a bad runtime configuration

## Manual Verification After Rollback

```bash
curl -fsS http://127.0.0.1/ >/dev/null
curl -fsS http://127.0.0.1/api/health >/dev/null
curl -fsS http://127.0.0.1/api/nodes >/dev/null
systemctl status loom.service
docker ps -a --format 'table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}'
```

## Notes

- `previous.env` is only a convenience snapshot; `last_successful.env` is the rollback source of truth.
- Legacy `sprite-*` services are intentionally removed during the Loom cutover and are not the primary rollback target anymore.
- If no successful release snapshot exists, the rollback script will stop the failed candidate stack and fail fast instead of pretending rollback succeeded.
