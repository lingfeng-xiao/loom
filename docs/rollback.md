# Loom Rollback

Rollback is executed from the server main worktree:

- repo root: `/home/lingfeng/loom`
- release records: `/home/lingfeng/loom/.release`
- default target: `HEAD~1`

## Host Command

```bash
cd /home/lingfeng/loom
./deploy/scripts/server-rollback.sh HEAD~1
```

## Development-time Guardrail

- Confirm the rollback target is a known good commit before writing anything.
- Prefer inspecting the latest release logs first.
- Keep rollback execution on the server main worktree so the deployed source and the git state stay aligned.

## What Rollback Does

1. Resolves the requested git ref in `/home/lingfeng/loom`.
2. Resets the server main worktree to that commit.
3. Reruns `server-deploy.sh`.
4. Reruns `server-healthcheck.sh`.
5. Writes a rollback report to `.release/rollback-<timestamp>/`.

## If Rollback Fails

1. Inspect `.release/rollback-<timestamp>/rollback.log`.
2. Inspect `.release/rollback-<timestamp>/deploy.log`.
3. Inspect `.release/rollback-<timestamp>/healthcheck.log`.
4. Confirm the target ref was a known good commit before retrying.
