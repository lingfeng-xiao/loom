# Loom

Loom is a project-first personal AI OS monorepo with:

- `loom-server`: Spring Boot API
- `loom-web`: React workspace UI
- `loom-node`: Java node agent
- GitHub Actions based build and release automation
- GitHub Models based real LLM integration

## Repo Layout

```text
apps/
  loom-server/
  loom-web/
  loom-node/
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
./apps/loom-server/mvnw -q test
./apps/loom-server/mvnw -q -DskipTests package
cd apps/loom-web && npm run build
./apps/loom-node/mvnw -q test
docker compose config
docker compose -f docker-compose.yml -f docker-compose.dev.yml config
docker compose -f deploy/compose/docker-compose.production.yml config
```

On Windows, replace `./apps/.../mvnw` with `.\apps\...\mvnw.cmd`.

## Production Deployment

Production deployments are standardized around:

- install root: `/opt/loom`
- systemd unit: `loom.service`
- public entrypoint: host port `80`
- release workflow: `.github/workflows/release.yml`

The release workflow now:

1. builds and pushes `loom-server`, `loom-web`, and `loom-node` images to GHCR
2. runs repository and GHCR hygiene checks before deployment proceeds
3. uploads a versioned deploy bundle to the target host
4. runs remote preflight, legacy backup, legacy retirement, rollout, smoke, and rollback logic through one remote release entrypoint
5. promotes the successful release into `/opt/loom/env/.env.production` and `/opt/loom/state/last_successful.env`

See:

- [Docs Index](./docs/README.md)
- [Deployment Guide](./docs/deployment.md)
- [Release Runbook](./docs/release-runbook.md)
- [Rollback Guide](./docs/rollback.md)
- [Postmortem: 2026-04-07 Deployment Hardening](./docs/postmortems/2026-04-07-deployment-hardening.md)

## Real LLM

Loom sends chat messages to an OpenAI-compatible `POST /chat/completions` endpoint.

Runtime settings:

- `LOOM_AI_PROVIDER_LABEL`
- `LOOM_AI_BASE_URL`
- `LOOM_AI_MODEL`
- `LOOM_AI_TEMPERATURE`
- `LOOM_AI_API_KEY`

Current production defaults target GitHub Models.

Important: GitHub Models should be treated as "free with rate limits and optional paid usage", not as an unlimited free production dependency.
Use a dedicated service credential for `LOOM_MODELS_API_KEY`, and separate GHCR service credentials for private image pulls.

Git pushes should use the SSH 443 remote:

`ssh://git@ssh.github.com:443/lingfeng-xiao/loom.git`

Official references:

- https://docs.github.com/en/billing/concepts/product-billing/github-models
- https://docs.github.com/en/github-models/prototyping-with-ai-models
