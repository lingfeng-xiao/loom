# Loom Git Workflow

## Branch Policy

- `main` is the only long-lived branch.
- Do not develop directly on `main`.
- Use short-lived branches with the `codex/` prefix unless a task explicitly requires another name.

## Branch Names

- `codex/feat-<topic>`
- `codex/fix-<topic>`
- `codex/refactor-<topic>`
- `codex/docs-<topic>`
- `codex/chore-<topic>`

Examples:

- `codex/feat-release-preflight`
- `codex/fix-smoke-retries`
- `codex/docs-runbook-hardening`

## Commits

Use Conventional Commits:

- `feat(server): add release readiness endpoint`
- `fix(deploy): harden remote preflight port checks`
- `docs(runbook): add GHCR credential policy`

## Pull Requests

- Describe scope, verification, and rollback risk.
- Wait for `Validate` to pass before merging.
- Keep production docs and release scripts in the same PR when deployment behavior changes.

## Remote Policy

- Fetch can stay on the standard GitHub URL.
- Pushes should use the SSH 443 remote:

`ssh://git@ssh.github.com:443/lingfeng-xiao/loom.git`
