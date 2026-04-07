# Template Docs Overview

This directory is the operating center for the clean infrastructure template.

## Start Here

- [Deployment Guide](./deployment.md)
- [Release Runbook](./release-runbook.md)
- [Rollback Guide](./rollback.md)
- [Node Agent Notes](./node-agent.md)
- [Frontend Baseline](./frontend-baseline.md)

## Rules

- Do not change release behavior without updating the deployment guide and runbook.
- Keep deploy scripts and compose files in sync with the current image names and env keys.
- Prefer generic placeholder guidance over product-specific examples.
