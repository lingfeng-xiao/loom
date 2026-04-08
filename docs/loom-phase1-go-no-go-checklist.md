# loom Phase 1 Go / No-Go Checklist

生效日期：2026-04-08

上游依据：

- `docs/requirements-pool-spec.md`
- `docs/loom-phase1-test-plan.md`
- `docs/loom-phase1-test-record.md`
- `docs/loom-phase1-risk-release-record.md`
- `docs/deployment.md`
- `docs/release-runbook.md`

## 1. 目的

本文件用于 `QA-005`，在进入发布候选或生产验证前，对当前 Phase 1 是否允许继续前进做最终判断。

## 2. 适用范围

- Phase 1 联调完成后
- 发布候选构建完成后
- 生产机 smoke 前
- 任何需要 PM 签字放行的窗口

## 3. Go / No-Go 门槛

### 3.1 必须为 Go 的条件

| 检查项 | Go 标准 | 证据来源 |
| --- | --- | --- |
| 需求池回填 | 对应需求状态、阻塞项、风险项已更新 | `requirements-pool-spec.md` |
| 合同冻结 | REST / SSE / 数据模型字段已冻结，且无歧义 | 架构与合同文档 |
| 本地验证 | `apps/server` 测试与 `apps/web` 构建通过 | 本地执行记录 |
| 联调记录 | 联调检查单已填写，问题与修复链路可回看 | `loom-phase1-joint-debug-checklist.md` |
| 回退准备 | 生产回退脚本和最近一次成功快照可用 | `rollback.md` / `deployment.md` |
| 风险控制 | 高风险项已明确 owner 与缓解动作 | `loom-phase1-risk-release-record.md` |

### 3.2 任一为 No-Go 的条件

| 检查项 | No-Go 表现 |
| --- | --- |
| 合同未冻结 | 事件名、字段名、错误口径仍然靠口头约定 |
| 测试缺失 | 核心接口或关键路径缺少可复核证据 |
| 回退不明 | 无法确认回退命令、回退快照或执行窗口 |
| 文档漂移 | 需求、实现、测试、风险之间出现明显不一致 |
| 生产写风险过高 | 仍需要临时改脚本或临时改环境才能验证 |

## 4. 发布前检查表

| 检查项 | 通过标准 | 责任人 |
| --- | --- | --- |
| P0 主链路 | 会话、提交、SSE、Trace、Context 至少一条链路可跑通 | QA / 后端 / 前端 |
| P1 配置面 | Settings / Capabilities 的读取结果可解释 | QA / 前端 |
| P2 占位面 | Files / Memory 等占位能力不会阻断主链路 | 前端 |
| 错误态 | 4xx / 5xx / SSE 断开可被解释或恢复 | 后端 / 前端 |
| 日志与回退 | 生产候选验证日志可留痕，且回退命令可执行 | PM |

## 5. 决策模板

| 字段 | 内容 |
| --- | --- |
| 日期 | YYYY-MM-DD |
| 发布候选 | commit / tag / branch |
| 覆盖需求 | 本次发布范围对应的需求 ID |
| 结论 | Go / No-Go |
| 阻塞项 | 具体问题与 owner |
| 风险 | 需要额外说明的边界 |
| 回退状态 | 是否已确认可回退 |
| PM 签字 | 通过 / 暂缓 |

## 6. 使用说明

- 任何一项进入 No-Go，默认暂停进入生产验证
- Go 结论必须同时写回测试记录和风险发布记录
- 生产验证结束后，必须补写最终结论和回退结果

## 7. 当前门禁状态（2026-04-08）

| 门禁项 | 当前判断 | 说明 |
| --- | --- | --- |
| 合同冻结文档 | 部分 Go | `loom-phase1-contract-freeze.md` 已可用，但仍需结合本轮 ARC 结果最终校准 |
| 本地后端验证 | Go | `apps/server` `./mvnw -q test` 已通过 |
| 本地前端验证 | Go | `apps/web` `npm run build` 已通过 |
| P0 主链路 | Go | `FE-002/003` + `BE-002/003` 已打通首版 |
| Context 真数据页面 | No-Go | `FE-004 / BE-004` 尚未完成 |
| Settings / Capabilities 真数据页面 | No-Go | `FE-005 / BE-005` 尚未完成 |
| 联调完整记录 | No-Go | `QA-004` 仍需补最终执行结论 |
| 生产机窗口 | No-Go | `ssh jd` 尚未进入只读检查窗口 |

结论：

- 当前允许继续并发开发与本地集成验证。
- 当前不允许进入生产机验证或最终发布候选。
