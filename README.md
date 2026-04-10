# Loom Server-Driven Workflow

`loom` now uses a server-first development model:

- business code is maintained on the server in `/home/lingfeng/loom`
- the server is the only writable source of truth for code and deploys
- local Codex stays in planner / reviewer / closer mode
- local work keeps docs, skills, helper scripts, and a read-only mirror for search and review

## Local responsibilities

- write briefs and review notes
- sync the server mirror into `.mirror/server-head/`
- sync the local Claude user config into the server `~/.claude/`
- trigger remote `claude -p` workers for single-task and parallel-task execution
- pull delegation artifacts back for review
- write `closeout.json` after review passes

Do not use the local workspace as the primary place to change business code.

## Server responsibilities

- keep the main git workspace in `/home/lingfeng/loom`
- run isolated task worktrees from `/home/lingfeng/worktrees`
- execute validation, deploy, healthcheck, and rollback
- store release evidence in `/home/lingfeng/loom/.release/<release-id>/`

## Common commands

```powershell
./.agents/skills/delegate-to-omc/scripts/sync-server-mirror.ps1
./.agents/skills/delegate-to-omc/scripts/sync-claude-user-config.ps1
./.agents/skills/delegate-to-omc/scripts/new-delegation.ps1 -TaskId "task-1" -Title "Short title"
./.agents/skills/delegate-to-omc/scripts/delegate-to-claude.ps1 -TaskId "task-1" -TaskFile ".\.delegations\task-1\brief.md"
./.agents/skills/delegate-to-omc/scripts/pull-delegation-artifacts.ps1 -TaskId "task-1"
./.agents/skills/delegate-to-omc/scripts/close-delegation.ps1 -TaskId "task-1"
```

```bash
ssh jd 'cd /home/lingfeng/loom && ./deploy/scripts/server-release.sh'
ssh jd 'cd /home/lingfeng/loom && ./deploy/scripts/server-rollback.sh HEAD~1'
```

See [docs/server-driven-workflow.md](docs/server-driven-workflow.md) for the full operating model.
