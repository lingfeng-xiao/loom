# loom Docs Overview

本目录自 2026-04-09 起进入“会话闭环收敛波次”维护口径。
当前唯一上游总计划为 [loom conversation closure plan](./loom-conversation-closure-plan.md)。

## Cleared

以下内容不再作为当前收敛波次的现行依据：

- 已删除产品概念对应的旧说明
- 仅服务旧页面、旧模式、旧数据源概念的文档
- 与当前产品级会话闭环无直接关系的扩展方向文档

## Remaining Valid Docs

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

- 当前收敛波次的需求、角色拆分、清理动作，统一以上游文档 [loom conversation closure plan](./loom-conversation-closure-plan.md) 为准。
- Current valid docs 只保留现行有效文档；过期文档必须删除，或明确转入 `archive` 语义后再保留。
- 新增文档必须写明生效日期、来源和是否属于 current valid docs。
- 任何删除类改动都必须在需求池中可追踪，不能只改代码不回填文档。
