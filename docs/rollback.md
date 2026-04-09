# Template Rollback

Rollback uses the last successful promoted env snapshot:

- current env: `/opt/template/env/.env.production`
- last successful env: `/opt/template/state/last_successful.env`

## Host Command

```bash
/opt/template/scripts/remote-rollback.sh
```

## Development-time Guardrail

在开发期使用 `ssh jd` 做生产验证时，任何写操作前都必须先确认：

- 当前操作由 PM 执行
- `/opt/template/state/last_successful.env` 存在
- 本轮变更已经在本地通过基础验证
- 本次验证目标和失败后的回退动作已记录

## What Rollback Does

1. Brings the stack back up with the image tags stored in `last_successful.env`.
2. Restores the active env file from the successful snapshot.
3. Restarts the systemd unit if it is active.

## If No Successful Snapshot Exists

The rollback script fails closed. In that case:

1. Inspect `/opt/template/state` for the last candidate env.
2. Inspect container logs and smoke-test output.
3. Re-run deploy with known good image tags.
