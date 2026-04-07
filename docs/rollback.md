# Template Rollback

Rollback uses the last successful promoted env snapshot:

- current env: `/opt/template/env/.env.production`
- last successful env: `/opt/template/state/last_successful.env`

## Host Command

```bash
/opt/template/scripts/remote-rollback.sh
```

## What Rollback Does

1. Brings the stack back up with the image tags stored in `last_successful.env`.
2. Restores the active env file from the successful snapshot.
3. Restarts the systemd unit if it is active.

## If No Successful Snapshot Exists

The rollback script fails closed. In that case:

1. Inspect `/opt/template/state` for the last candidate env.
2. Inspect container logs and smoke-test output.
3. Re-run deploy with known good image tags.
