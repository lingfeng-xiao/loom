# Template Infrastructure Monorepo

A clean three-app monorepo template with:

- `apps/server`: Spring Boot API with Flyway, JDBC, and node registration endpoints
- `apps/web`: React + Vite infrastructure dashboard shell
- `apps/node`: Java node agent that runs probes and reports heartbeat state
- `packages/contracts`: shared TypeScript contracts for the template surface
- Docker Compose, release scripts, and GitHub Actions for CI/CD

## Repository Layout

```text
apps/
  server/
  web/
  node/
packages/
  contracts/
deploy/
  compose/
  scripts/
  systemd/
docs/
```

## Common Commands

```bash
./apps/server/mvnw -q test
./apps/server/mvnw -q -DskipTests package
./apps/node/mvnw -q test
./apps/node/mvnw -q -DskipTests package
cd apps/web && npm install && npm run build
npx -p typescript@5.6.3 tsc -p packages/contracts/tsconfig.json --noEmit
docker compose config
docker compose -f docker-compose.yml -f docker-compose.dev.yml config
docker compose -f deploy/compose/docker-compose.production.yml --env-file .env.example config
```

## What This Template Keeps

- a generic API bootstrap/settings surface
- node registration, heartbeat, and probe persistence
- a minimal web shell for setup, release metadata, and node inventory
- full image build, release, deploy, smoke-test, and rollback scaffolding

## What You Are Expected To Replace

- the placeholder dashboard in `apps/web/src`
- the placeholder setup tasks and extension point descriptions from the API bootstrap payload
- any server modules beyond the infrastructure endpoints
- any node probes beyond the default server/web health checks

## Deployment Defaults

- install root: `/opt/template`
- systemd unit: `template.service`
- public entrypoint: host port `80`
- registry target: `ghcr.io/<owner>/template-server|template-web|template-node`

See [docs/README.md](./docs/README.md) for the deployment guide, release runbook, rollback notes, and customization references.
