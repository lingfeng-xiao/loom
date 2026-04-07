# Loom Git 工作流规范

## 主干策略

- 默认主干为 `main`
- 不直接在 `main` 上开发
- 所有功能、修复、重构都先走分支，再通过 PR 合并

## 分支命名

- 功能：`codex/feat-<topic>`
- 修复：`codex/fix-<topic>`
- 重构：`codex/refactor-<topic>`
- 文档：`codex/docs-<topic>`
- 运维：`codex/chore-<topic>`

示例：

- `codex/feat-db-compose`
- `codex/fix-node-probes`
- `codex/docs-rollout`

## 提交规范

统一使用 Conventional Commits：

- `feat(server): persist projects and conversations to mysql`
- `fix(node): include last error in heartbeat payload`
- `refactor(web): reduce chat workspace explanatory copy`
- `docs(deploy): document jd cutover and rollback`

## PR 规范

- PR 必须说明改动范围、验证方式、风险点
- 合并前至少通过 `validate` 流水线
- `main` 只接受通过 CI 的 PR 合并

## 仓库目标

- GitHub 仓库目标固定为 `lingfeng-xiao/loom`
- 推荐 SSH 远端：`git@github.com:lingfeng-xiao/loom.git`
