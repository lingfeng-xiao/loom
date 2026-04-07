# Template Deployment

The production template deploys into:

- install root: `/opt/template`
- compose file: `/opt/template/compose/docker-compose.production.yml`
- environment file: `/opt/template/env/.env.production`
- state directory: `/opt/template/state`
- systemd unit: `template.service`

## Runtime Topology

- `template-edge`: public nginx entrypoint on port `80`
- `template-web`: internal static web container
- `template-server`: internal Spring Boot API
- `template-node`: internal probe/heartbeat agent
- `template-mysql`: internal MySQL database

Only the edge proxy binds the host port directly in production.

## Release Flow

1. GitHub Actions builds the server, web, and node images.
2. The release workflow pushes images to GHCR.
3. A deploy bundle is uploaded to the target host.
4. `remote-release.sh` renders a candidate env file and runs preflight checks.
5. The candidate deployment is started, smoke-tested, and then promoted to the current env snapshot.
6. The systemd unit is reloaded or started after the deployment succeeds.

## Files That Must Stay Aligned

- `deploy/compose/docker-compose.production.yml`
- `deploy/compose/edge/nginx.conf`
- `deploy/systemd/template.service`
- `deploy/scripts/remote-*.sh`
- `.env.example`

## Required Secrets

- `DEPLOY_HOST`
- `DEPLOY_USER`
- `DEPLOY_SSH_PRIVATE_KEY`
- `GHCR_USERNAME`
- `GHCR_TOKEN`

## Notes

- The host must have Docker Engine with Docker Compose enabled.
- The deploy user must be able to install files under `/opt/template` and manage the systemd unit.
- If you change image names or service names, update the release workflow, smoke tests, and rollback docs together.
