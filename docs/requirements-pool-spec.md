# Loom Requirements Pool

Effective date: 2026-04-11

## Workflow Evolution Wave (2026-04-11)

Source: `docs/workflow-evolution-plan.md`

### Maintenance rules

- This wave upgrades the development and delivery workflow, not the user-facing product.
- Each requirement must reference `docs/workflow-evolution-plan.md` as the upstream source.
- Work must use isolated server worktrees under `/home/lingfeng/worktrees`.
- The frozen runner baseline must remain available until the new workflow passes rollout gates.
- Monitoring artifacts are part of acceptance, not optional follow-up work.

### ARC Requirements

| Requirement ID | Priority | Status | Title | Source | Dependency | Acceptance |
| --- | --- | --- | --- | --- | --- | --- |
| ARC-Workflow-001 | P0 | Ready | Define workflow state model, event model, and task lifecycle. | `docs/workflow-evolution-plan.md` | None | The state model covers planned, split, dispatched, running, timeout, review, retry, validation, deploy, closeout, and abandoned states. |
| ARC-Workflow-002 | P0 | Ready | Define concurrency, locking, and protected deploy gates. | `docs/workflow-evolution-plan.md` | ARC-Workflow-001 | Worktree locks, file-surface locks, release locks, deploy gate, and rollback gate are defined with acceptance criteria. |

### OPS Requirements

| Requirement ID | Priority | Status | Title | Source | Dependency | Acceptance |
| --- | --- | --- | --- | --- | --- | --- |
| OPS-Workflow-001 | P0 | Ready | Migrate toward Codex direct orchestration of server Bash runners. | `docs/workflow-evolution-plan.md` | ARC-Workflow-001 | The Bash-only target runner model and legacy fallback strategy are documented and ready for implementation. |
| OPS-Workflow-002 | P0 | Ready | Implement release evidence and rollback linkage for workflow closeout. | `docs/workflow-evolution-plan.md` | OPS-Workflow-001, ARC-Workflow-002 | Closeout can reference release id, rollback ref, validation result, deploy result, and failure state. |

### QA Requirements

| Requirement ID | Priority | Status | Title | Source | Dependency | Acceptance |
| --- | --- | --- | --- | --- | --- | --- |
| QA-Workflow-001 | P0 | Ready | Establish the workflow regression and failure-injection matrix. | `docs/workflow-evolution-plan.md` | ARC-Workflow-001, OPS-Workflow-001 | Test coverage includes dispatch, concurrency locks, timeout, idle timeout, review rejection, validation failure, deploy failure, rollback, and telemetry rollup. |

### OBS Requirements

| Requirement ID | Priority | Status | Title | Source | Dependency | Acceptance |
| --- | --- | --- | --- | --- | --- | --- |
| OBS-Workflow-001 | P0 | Ready | Define telemetry events, task summary, and daily rollup schema. | `docs/workflow-evolution-plan.md` | ARC-Workflow-001 | Event and rollup schemas cover phase duration, retries, timeouts, review rejects, validation failures, deploy failures, rollback counts, and closeout outcome. |
| OBS-Workflow-002 | P1 | Ready | Define workflow optimization cadence and reporting outputs. | `docs/workflow-evolution-plan.md` | OBS-Workflow-001 | Daily rollup, weekly optimization report, and monthly threshold review are documented with required metrics. |

### DOC Requirements

| Requirement ID | Priority | Status | Title | Source | Dependency | Acceptance |
| --- | --- | --- | --- | --- | --- | --- |
| DOC-Workflow-001 | P1 | Ready | Update operator-facing workflow docs and skill instructions. | `docs/workflow-evolution-plan.md` | OPS-Workflow-001, QA-Workflow-001, OBS-Workflow-001 | README, server-driven workflow docs, skill docs, and operator guide all describe the same workflow and do not contradict the frozen-runner migration strategy. |
