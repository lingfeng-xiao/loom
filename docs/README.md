# Template Docs

## Core References

- [Deployment Guide](./deployment.md)
- [Release Runbook](./release-runbook.md)
- [Rollback Guide](./rollback.md)
- [Node Agent Notes](./node-agent.md)
- [Frontend Baseline](./frontend-baseline.md)
- [Git Workflow](./git-workflow.md)

## Operating Rules

- Keep release scripts, compose files, and this documentation aligned.
- Update `.env.example` whenever runtime variables change.
- Keep rollback instructions executable for the current `/opt/template` layout.
- Treat the API bootstrap payload as the source of truth for what the default web shell renders.
