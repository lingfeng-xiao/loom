# Loom Docs Overview

This directory is the operating center for Loom production delivery and engineering follow-up.

## Start Here

- [Deployment Guide](./deployment.md)
- [Release Runbook](./release-runbook.md)
- [Rollback Guide](./rollback.md)
- [Known Issues & Engineering Backlog](./known-issues.md)
- [Postmortem: 2026-04-07 Deployment Hardening](./postmortems/2026-04-07-deployment-hardening.md)

## Rules

- Do not change production release behavior without updating the deployment guide and runbook.
- Keep both preflight scripts aligned with the current release policy and production layout.
- Keep rollback instructions executable and aligned with the current `/opt/loom` layout.
- Convert repeated operational pain into scripts, checks, or documented policy instead of leaving it as tribal knowledge.
