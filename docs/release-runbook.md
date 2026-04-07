# Template Release Runbook

## Before You Release

1. Make sure the local validation commands pass.
2. Confirm `.env.example` still matches the runtime variables used by compose and scripts.
3. Confirm the target host has Docker, Docker Compose, and systemd available.
4. Confirm GitHub Actions secrets for deploy and GHCR are configured.

## What The Workflow Does

1. Runs `deploy/scripts/release-preflight.sh`.
2. Builds `template-server`, `template-web`, and `template-node`.
3. Pushes the images to GHCR.
4. Uploads the deploy bundle to the target host.
5. Executes `remote-release.sh` on the host.

## Manual Remote Release

If you need to run the host-side flow manually:

```bash
DEPLOY_STAGE_ROOT="$HOME/template-deploy" \
INSTALL_ROOT=/opt/template \
TEMPLATE_SERVER_IMAGE=ghcr.io/<owner>/template-server:<sha> \
TEMPLATE_WEB_IMAGE=ghcr.io/<owner>/template-web:<sha> \
TEMPLATE_NODE_IMAGE=ghcr.io/<owner>/template-node:<sha> \
GHCR_USERNAME=<username> \
GHCR_TOKEN=<token> \
/opt/template/scripts/remote-release.sh
```

## Success Criteria

- `remote-preflight.sh` passes.
- `remote-deploy.sh` starts the candidate stack.
- `remote-smoke-test.sh` succeeds for the web entrypoint and API health endpoints.
- `/opt/template/state/last_successful.env` is updated.
