# Loom Deployment

## Production Shape

Loom production is standardized around:

- install root: `/opt/loom`
- public entrypoint: host port `80`
- release compose: `/opt/loom/compose/docker-compose.production.yml`
- environment file: `/opt/loom/env/.env.production`
- release state: `/opt/loom/state/`
- backup root: `/opt/loom/backups/`
- systemd unit: `loom.service`

The runtime stack is:

- `loom-edge`: public Nginx edge proxy
- `loom-web`: internal web container
- `loom-server`: internal API container
- `loom-node`: internal node agent
- `loom-mysql`: MySQL 8.2

Only `loom-edge` binds a host port. `loom-web`, `loom-server`, `loom-node`, and `loom-mysql` stay on the internal Docker network.

## Release Flow

`release.yml` now performs a single remote release entrypoint:

1. run release preflight against remote Git refs and GHCR package hygiene
2. build and push application images
3. upload a versioned deploy bundle to the staging root on the server
4. sync bundle content into `/opt/loom`
5. back up legacy services
6. retire legacy `sprite-*` and `template-*` containers
7. run remote preflight
8. deploy the candidate release
9. smoke test `/`, `/api/health`, and `/api/nodes`
10. promote the candidate env file to `/opt/loom/env/.env.production`
11. update `/opt/loom/state/last_successful.env`
12. reload or start `loom.service`
13. roll back automatically if deploy or smoke fails

## Release Bundle Contents

The release bundle contains:

- `deploy/compose/docker-compose.production.yml`
- `deploy/compose/edge/nginx.conf`
- `deploy/systemd/loom.service`
- `deploy/scripts/remote-*.sh`

The bundle is uploaded to a user-writable staging root first, then copied into `/opt/loom`.

## Preflight Rules

Preflight is split into two scripts:

- `deploy/scripts/release-preflight.sh`: checks GitHub refs, GHCR package hygiene, and the repository push-remote policy
- `/opt/loom/scripts/remote-preflight.sh`: checks host state before the cutover

`remote-preflight.sh` is expected to fail when:

- legacy `sprite-*` or `template-*` containers still exist
- legacy Docker networks still exist
- port `80` is occupied by something other than `loom-loom-edge-1`
- Docker or Docker Compose is unavailable
- the staging root or install root is not writable
- `loom.service` is already in a failed state
- the compose config cannot be rendered once the compose file and env file are available

The remote script can be run independently before a manual cutover. If the bundle has not been synced yet, it still validates host readiness and skips the compose render check.

## Environment File Rules

`/opt/loom/env/.env.production` is the single runtime source of truth.

The release script rewrites the production env file with:

- absolute host paths under `/opt/loom/data/...`
- host port `80`
- the candidate image tags
- the active LLM provider settings

Do not treat `.env` as a shell script. All scripts must parse it explicitly as `key=value`.

## LLM Credentials

Production currently uses GitHub Models through the standard Loom OpenAI-compatible gateway.

Required runtime keys:

- `LOOM_AI_PROVIDER_LABEL`
- `LOOM_AI_BASE_URL`
- `LOOM_AI_MODEL`
- `LOOM_AI_TEMPERATURE`
- `LOOM_AI_API_KEY`

The release pipeline supports injecting `LOOM_MODELS_API_KEY` from GitHub Actions secrets into the candidate env file.

Credential rules:

- Git pushes should use `ssh://git@ssh.github.com:443/<owner>/<repo>.git`
- GHCR pulls should use a dedicated read-only service credential when packages are private
- `LOOM_MODELS_API_KEY` should be a dedicated production credential, not a personal long-lived PAT
- keep Actions secrets scoped to the minimum permissions needed for release

Important cost note:

- GitHub Models has free, rate-limited access for prototyping
- usage and rate limits vary by model and plan
- paid usage can be enabled

Official references:

- https://docs.github.com/en/billing/concepts/product-billing/github-models
- https://docs.github.com/en/github-models/prototyping-with-ai-models
