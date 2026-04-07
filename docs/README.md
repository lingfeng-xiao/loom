# Loom Docs

## Core Docs

- [Deployment Guide](./deployment.md)
- [Release Runbook](./release-runbook.md)
- [Rollback Guide](./rollback.md)
- [Known Issues & Engineering Backlog](./known-issues.md)
- [Postmortem: 2026-04-07 Deployment Hardening](./postmortems/2026-04-07-deployment-hardening.md)
- [Git Workflow](./git-workflow.md)
- [Node Agent Notes](./node-agent.md)
- [Frontend Baseline](./frontend-baseline.md)

## Operating Rules

- Treat `docs/deployment.md` as the source of truth for production layout and release flow.
- Treat `deploy/scripts/release-preflight.sh` and `/opt/loom/scripts/remote-preflight.sh` as mandatory gates before manual cutover.
- Record deployment regressions and follow-up hardening items in `docs/known-issues.md`.
- Record major incidents and decision-quality lessons in `docs/postmortems/`.
- Keep the runbook current whenever release steps, rollback logic, or required secrets change.
