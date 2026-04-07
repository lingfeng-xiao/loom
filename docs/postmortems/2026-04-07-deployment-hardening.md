# Postmortem: 2026-04-07 Deployment Hardening

## Summary

This hardening round started as a repo bootstrap and release setup task, then exposed multiple deployment-quality problems that were not visible from local builds alone.

## Root Causes

1. Old repo history was not fully cleaned before the new Loom monorepo cutover.
2. The target host still had `sprite-proxy` bound to port `80`.
3. Early release scripts treated `.env` files like shell scripts even when values contained spaces.
4. Smoke checks assumed the stack would be ready immediately after `docker compose up`.

## What Changed

- standardized production under `/opt/loom`
- introduced a dedicated edge proxy service for public traffic
- added release preflight for refs, packages, and remote host readiness
- moved release logic into versioned remote scripts
- added strict remote preflight
- added legacy backup and cleanup steps
- added release rollback snapshots
- added systemd ownership of the production stack
- documented GitHub Models billing assumptions explicitly

## What Was Not Good Enough

- The initial rollout depended on a personal token path for GitHub Models.
- Port ownership was discovered during deployment instead of before deployment.
- Repo cleanup and runtime cleanup were planned too late instead of being part of the first cutover checklist.

## Speed Improvements For Next Time

- Always run a preflight checklist before writing release YAML.
- Inspect host ports, legacy containers, and existing proxies before deciding the target public entrypoint.
- Treat rollback and smoke as first-class deliverables from the start.
- Standardize bundle layout and remote script entrypoints before the first production push.

## Permanent Follow-Ups

- rotate the placeholder LLM token to a service credential
- add TLS and domain support
- keep postmortem findings synced into `docs/known-issues.md`
