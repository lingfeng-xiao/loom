# Server Release Runbook

## Before You Release

1. Confirm `/home/lingfeng/loom` is the writable source of truth and has a valid git `HEAD`.
2. Run `./deploy/scripts/server-prepare-main-worktree.sh` from `/home/lingfeng/loom`.
3. Run `./deploy/scripts/server-install-loom-service.sh` from `/home/lingfeng/loom`.
4. Confirm `/home/lingfeng/loom/.env` matches the variables expected by `docker-compose.yml`.
5. Confirm the host can run `docker compose`, `claude`, and, for parallel tasks, `omc team`.
6. Confirm `/home/lingfeng/worktrees` exists for isolated task worktrees.
7. Confirm there is a rollback target, typically `HEAD~1` or the previous known good commit.

## Fixed Server Release Flow

Run the release from the server main worktree:

```bash
ssh jd 'cd /home/lingfeng/loom && ./deploy/scripts/server-release.sh'
```

The release script performs four steps in order:

1. `deploy/scripts/server-validate.sh`
2. `deploy/scripts/server-prepare-images.sh`
3. `deploy/scripts/server-deploy.sh`
4. `deploy/scripts/server-healthcheck.sh`
5. `deploy/scripts/server-release-report.sh`

`deploy/scripts/server-validate.sh` is now the minimum formal-release gate only:

1. `git status` must be clean on branch `main`
2. `docker compose config` must pass against `/home/lingfeng/loom/docker-compose.yml`
3. `systemctl cat loom.service` must reference only `/home/lingfeng/loom/docker-compose.yml` and `/home/lingfeng/loom/.env`
4. `sudo -n true` must succeed
5. `mihomo.service` must be active and Docker daemon must expose the 127.0.0.1:7890 proxy

If you want a heavier pre-release confidence pass, run:

```bash
ssh jd 'cd /home/lingfeng/loom && ./deploy/scripts/server-validate-full.sh'
```

## Release Evidence

Each release writes `/home/lingfeng/loom/.release/<release-id>/` and includes at least:

- `release.json`
- `validate.log`
- `prepare-images.log`
- `deploy.log`
- `healthcheck.log`
- `rollback.md`

If `server-validate-full.sh` is run manually, it writes `validate-full.log`.

`release.json` records:

- `release_id`
- `started_at`
- `finished_at`
- `operator`
- `repo_head`
- `validated`
- `sudo_ready`
- `proxy_ready`
- `images_ready`
- `deployed`
- `healthcheck_passed`
- `rollback_ready`
- `release_mode`
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
- `lingfeng` is expected to have passwordless sudo for deploy operations.
- `mihomo.service` is part of the formal Docker Hub access baseline; do not stop it during routine release validation.
- If `claude -p` still fails after syncing the real user-level config, record the degradation and continue release work. Delegation health is not release-blocking for this cutover.
- After the first successful release, run `./deploy/scripts/server-strong-cleanup.sh <release-id>` to remove `/opt/loom`.
