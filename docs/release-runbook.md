# Server Release Runbook

## Before You Release

1. Confirm `/home/lingfeng/loom` is the writable source of truth and has a valid git `HEAD`.
2. Confirm `/home/lingfeng/loom/.env` matches the variables expected by `docker-compose.yml`.
3. Confirm the host can run `docker compose`, `claude`, and, for parallel tasks, `omc team`.
4. Confirm `/home/lingfeng/worktrees` exists for isolated task worktrees.
5. Confirm there is a rollback target, typically `HEAD~1` or the previous known good commit.

## Fixed Server Release Flow

Run the release from the server main worktree:

```bash
ssh jd 'cd /home/lingfeng/loom && ./deploy/scripts/server-release.sh'
```

The release script performs four steps in order:

1. `deploy/scripts/server-validate.sh`
2. `deploy/scripts/server-deploy.sh`
3. `deploy/scripts/server-healthcheck.sh`
4. `deploy/scripts/server-release-report.sh`

## Release Evidence

Each release writes `/home/lingfeng/loom/.release/<release-id>/` and includes at least:

- `release.json`
- `validate.log`
- `deploy.log`
- `healthcheck.log`
- `rollback.md`

`release.json` records:

- `release_id`
- `started_at`
- `finished_at`
- `operator`
- `repo_head`
- `validated`
- `deployed`
- `healthcheck_passed`
- `rollback_ready`
- `status`

## Rollback

Rollback is server-first and uses the main worktree:

```bash
ssh jd 'cd /home/lingfeng/loom && ./deploy/scripts/server-rollback.sh HEAD~1'
```

The rollback command resets the server worktree to the requested ref, redeploys, reruns healthchecks, and writes a rollback report under `.release/rollback-<timestamp>/`.

## Operating Rule

- Prefer read-only checks first: `git status`, `docker compose ps`, `docker ps`, and health endpoints.
- Do not use legacy GitHub Actions, GHCR bundles, or `/opt/loom`-style side channels.
- If `loom.service` is present but passwordless `sudo` is unavailable, the deploy script falls back to direct `docker compose` and records that fact in `deploy.log`.
