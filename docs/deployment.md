# loom Deployment

## Local Acceptance Deployment

Use this flow when you want to validate the current loom shell locally without relying on an external MySQL instance.

Backend:

1. Run from `apps/server` with the `local` profile.
2. The `local` profile uses an embedded H2 database stored under `.loom-local`.
3. Default API URL: `http://127.0.0.1:8080`

Frontend:

1. Run from `apps/web` with Vite dev server.
2. Default acceptance URL: `http://127.0.0.1:4173`
3. `/api` requests proxy to the local backend during development.

Recommended commands:

```powershell
cd C:\Users\16343\Desktop\loom\apps\server
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

```powershell
cd C:\Users\16343\Desktop\loom\apps\web
npm run dev -- --host 127.0.0.1 --port 4173
```

## Server Deployment Baseline

The server host is the only writable deployment source:

- repo root: `/home/lingfeng/loom`
- compose file: `/home/lingfeng/loom/docker-compose.yml`
- environment file: `/home/lingfeng/loom/.env`
- systemd unit: `loom.service`
- worktree root: `/home/lingfeng/worktrees`
- release records: `/home/lingfeng/loom/.release`

## Development-time Production Access

- Prefer read-only checks first: `docker compose ps`, `docker ps`, health endpoints, and logs.
- Keep all writes inside the fixed `validate -> deploy -> healthcheck -> report` chain.
- Do not use legacy GitHub Actions, GHCR bundles, or `/opt/template` scripts.

## Runtime Topology

- `loom-web`: public web container on `${LOOM_PUBLIC_PORT:-80}`
- `loom-server`: Spring Boot API on `${LOOM_SERVER_PORT:-8080}`
- `loom-node`: node agent for probes and heartbeat
- `loom-mysql`: MySQL database
- runtime state under `/home/lingfeng/loom`: `.env`, `logs/`, `vault/`, `.delegations/`, `.release/`, `.tmp/`

## Release Flow

1. Run `deploy/scripts/server-validate.sh` in `/home/lingfeng/loom`.
2. Run `deploy/scripts/server-deploy.sh` in `/home/lingfeng/loom`.
3. Run `deploy/scripts/server-healthcheck.sh` in `/home/lingfeng/loom`.
4. Run `deploy/scripts/server-release-report.sh` in `/home/lingfeng/loom`.

## Files That Must Stay Aligned

- `docker-compose.yml`
- `deploy/systemd/loom.service`
- `deploy/scripts/server-*.sh`
- `.env.example`

## Notes

- Local acceptance still prefers the `local` profile so reviewers can start the backend without provisioning MySQL first.
- If service names, ports, or startup modes change, update this file together with the release and rollback docs.
