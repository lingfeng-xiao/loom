# Loom Release Runbook

## First-Time Setup

1. Ensure the target host is reachable by SSH.
2. Ensure Docker and Docker Compose are installed on the host.
3. Ensure the release staging root is writable by the deploy user.
4. Ensure Git pushes use `ssh://git@ssh.github.com:443/lingfeng-xiao/loom.git`.
5. Configure GitHub Actions secrets:
   - `DEPLOY_HOST`
   - `DEPLOY_USER`
   - `DEPLOY_SSH_PRIVATE_KEY`
   - recommended for private image pulls: `GHCR_USERNAME`
   - recommended for private image pulls: `GHCR_TOKEN`
   - optional: `DEPLOY_PORT`
   - optional: `DEPLOY_ROOT`
   - optional: `DEPLOY_SSH_KNOWN_HOSTS`
   - recommended: `LOOM_MODELS_API_KEY`

Credential policy:

- `GHCR_TOKEN` should belong to a dedicated package-pull identity with the minimum package scope required.
- `LOOM_MODELS_API_KEY` should be a dedicated production credential, not a developer personal token.
- Do not reuse one token for Git push, GHCR, and model inference.

## Release Preflight

Run before any first deploy or manual cutover:

```bash
bash deploy/scripts/release-preflight.sh
```

This checks:

- remote Git refs
- GHCR package hygiene
- the push remote format

## Standard Release

1. Push to `main`.
2. Confirm `deploy/scripts/release-preflight.sh` passes.
3. Wait for `Validate`.
4. Wait for `Release`.
5. Confirm:
   - `/` returns 200
   - `/api/health` returns 200
   - `/api/nodes` returns 200
   - a real chat reply is generated

## Manual Host Preflight

Run on the target host:

```bash
DEPLOY_STAGE_ROOT=~/loom-deploy INSTALL_ROOT=/opt/loom /opt/loom/scripts/remote-preflight.sh
```

## Manual Release

Run on the target host after updating the bundle and image tags:

```bash
DEPLOY_STAGE_ROOT=~/loom-deploy \
INSTALL_ROOT=/opt/loom \
GHCR_USERNAME=<service-user> \
GHCR_TOKEN=<service-token> \
LOOM_MODELS_API_KEY=<service-model-key> \
LOOM_SERVER_IMAGE=ghcr.io/<owner>/loom-server:<sha> \
LOOM_WEB_IMAGE=ghcr.io/<owner>/loom-web:<sha> \
LOOM_NODE_IMAGE=ghcr.io/<owner>/loom-node:<sha> \
/opt/loom/scripts/remote-release.sh
```

## Rollback

```bash
/opt/loom/scripts/remote-rollback.sh
```

## Changing Port Or Entry

- Host port changes belong in the production env file, not in container definitions.
- Only `loom-edge` should own the public host port.
- Before reusing `80` or `443`, confirm there is no legacy proxy or unrelated service still attached to that port.
- After a port change, rerun `/opt/loom/scripts/remote-preflight.sh` and verify `ss -ltnp`.

## Rotating LLM Credentials

1. Create a new service credential outside the release flow.
2. Update the GitHub Actions secret `LOOM_MODELS_API_KEY`.
3. Trigger a release so the candidate env file is regenerated.
4. Verify a real chat request still returns a model response.
