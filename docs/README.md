# loom Docs

## Current Status

自 2026-04-09 起，文档体系以“项目内产品级会话闭环”为唯一主线。
当前唯一上游总计划文档为 [loom conversation closure plan](./loom-conversation-closure-plan.md)。

当前有效主线包括：

- `loom-conversation-closure-plan.md`
- `requirements-pool-spec.md`
- `deployment.md`
- `rollback.md`
- `release-runbook.md`
- `loom-phase1-test-plan.md`
- `loom-phase1-test-record.md`
- `loom-phase1-risk-release-record.md`

## Temporarily Cleared Or Out Of Scope

- 已删除产品概念对应的历史文档
- 仅服务 `Chat / Plan / Action / Review` 的说明
- 仅服务“本地快照 / 远端数据 / fallback”概念的说明
- 不直接服务当前会话闭环的扩展方向文档

## Still Valid

- [loom conversation closure plan](./loom-conversation-closure-plan.md)
- [development spec](./development-spec.md)
- [deployment](./deployment.md)
- [git workflow](./git-workflow.md)
- [loom Phase 1 architecture design](./loom-phase1-architecture-design.md)
- [loom Phase 1 kickoff board](./loom-phase1-kickoff-board.md)
- [loom Phase 1 test plan](./loom-phase1-test-plan.md)
- [loom Phase 1 test record](./loom-phase1-test-record.md)
- [loom Phase 1 joint debug checklist](./loom-phase1-joint-debug-checklist.md)
- [loom Phase 1 go / no-go checklist](./loom-phase1-go-no-go-checklist.md)
- [loom Phase 1 frontend smoke record](./loom-phase1-frontend-smoke-record.md)
- [loom Phase 1 risk and release record](./loom-phase1-risk-release-record.md)
- [loom Spring Boot backend module design](./loom-springboot-backend-module-design.md)
- [loom Java package structure](./loom-java-package-structure.md)
- [requirements pool spec](./requirements-pool-spec.md)
- [loom V1 role breakdown](./loom-v1-role-breakdown.md)
- [release runbook](./release-runbook.md)
- [rollback](./rollback.md)
- [node agent](./node-agent.md)

## Rule

- 当前收敛波次只认 [loom conversation closure plan](./loom-conversation-closure-plan.md) 这一份上游总计划文档。
- `requirements-pool-spec.md` 中的本波次需求必须按 `ARC / FE / BE / PM / QA / OPS` 角色拆分维护，并统一引用该主计划。
- current valid docs 只保留现行文档；过期文档必须删除，或明确转入 archive 语义后再保留。
- 新增或更新文档时，必须同步说明来源、生效日期、是否影响需求池、是否影响发布与验收。
