# Loom Known Issues & Engineering Backlog

Last updated: 2026-04-08

## Open

| ID | Title | Priority | Status | Current State | Next Action |
| --- | --- | --- | --- | --- | --- |
| `LOOM-OPS-007` | Dedicated production credentials still need final rotation | P1 | Open | The release pipeline now supports distinct secret paths for GitHub Models and GHCR, but the final service-owned credential rotation still needs to be completed outside this repo. | Replace any remaining personal or placeholder credentials with dedicated service-owned secrets and remove legacy tokens. |
| `LOOM-OPS-008` | HTTPS and domain cutover not yet done | P2 | Open | Loom now owns the standard production path and host port `80`, but TLS is still intentionally out of scope for this round. | Add domain, certificate management, and `443` support in the next deployment hardening pass. |

## Resolved This Round

| ID | Title | Priority | Status | Resolution |
| --- | --- | --- | --- | --- |
| `LOOM-OPS-001` | Port `80` conflict delayed production cutover | P0 | Resolved | Release flow now backs up and retires legacy `sprite-*` and `template-*` containers before the Loom cutover, then verifies port ownership in preflight. |
| `LOOM-OPS-002` | Release logic was spread across long SSH inline commands | P1 | Resolved | Release now executes one remote entrypoint script that owns backup, cleanup, preflight, deploy, smoke, and rollback. |
| `LOOM-OPS-003` | `.env` parsing was unsafe for values with spaces | P1 | Resolved | Release and smoke logic now parse env files explicitly as `key=value` instead of shell-sourcing them. |
| `LOOM-OPS-004` | Smoke checks were too eager during container warmup | P1 | Resolved | Smoke checks now retry during startup and are treated as part of release readiness, not a single-shot curl. |
| `LOOM-OPS-005` | Production layout was not standardized | P1 | Resolved | Deploy root, env path, state path, backup path, compose file path, and systemd unit are now standardized under `/opt/loom`. |
| `LOOM-OPS-006` | Legacy repo and package leftovers were still visible | P1 | Resolved | The cleanup plan now includes deleting obsolete refs, legacy container packages, and old server runtime stacks after verification. |
