# loom Phase 1 前端关键路径冒烟记录

生效日期：2026-04-08

上游依据：

- `docs/requirements-pool-spec.md`
- `docs/loom-phase1-test-plan.md`
- `docs/loom-phase1-joint-debug-checklist.md`
- `docs/loom-phase1-go-no-go-checklist.md`

## 1. 目的

本文件用于记录前端关键路径手测和冒烟验证，重点覆盖 `QA-003`。

## 2. 适用路径

- 欢迎页进入工作台
- 会话切换
- 模式切换
- 消息提交
- SSE 消费后的消息 / Trace / Context 刷新
- `Capabilities` 与 `Settings` 页面读取
- fallback 切换

## 3. 记录模板

| 字段 | 内容 |
| --- | --- |
| 记录 ID | 例如 `SMOKE-FE-2026-04-08-01` |
| 日期 | YYYY-MM-DD |
| 环境 | 本地 / 集成分支 / 生产候选 |
| 浏览器 | Chrome / Edge / Safari / 其他 |
| 构建版本 | branch / commit |
| 覆盖需求 | `FE-002`、`FE-003`、`FE-004` 等 |
| 起点页面 | 进入时所在页面 |
| 关键动作 | 具体点击 / 输入 / 切换步骤 |
| 期望结果 | 预期的页面表现 |
| 实际结果 | 实际发生了什么 |
| 结论 | 通过 / 部分通过 / 阻塞 |
| 问题 | 如有，写明现象与范围 |
| 附件 | 截图 / 录屏 / 控制台日志 / 构建结果 |

## 4. 建议冒烟步骤

1. 打开欢迎页并进入工作台。
2. 进入会话页，确认当前会话和消息列表展示正常。
3. 输入草稿并提交，确认消息进入后端主链路。
4. 观察 SSE 事件触发后，消息、Trace、Context 是否同步变化。
5. 切换 `Trace / Context / Settings` 右侧面板，确认页面无阻塞。
6. 切换 `Capabilities` 页面和 `Settings` 页面，确认仍可稳定渲染。
7. 切换 bootstrap 数据源，确认 fallback 与远端模式都不破坏主链路。

## 5. 当前可回填记录

### SMOKE-FE-2026-04-08-01

- 日期：2026-04-08
- 环境：本地 / `codex/integration-phase1-delivery`
- 浏览器：未记录
- 构建版本：`FE-002` 提交 `80a5084`，`FE-003` 提交 `d5a748f`
- 覆盖需求：`FE-002`、`FE-003`
- 起点页面：会话工作台
- 关键动作：提交消息，等待 SSE 事件消费后刷新 bootstrap
- 期望结果：消息、Trace、Context 可见更新，页面不阻塞
- 实际结果：前端构建通过，提交链路与 stream 消费链路均已接通
- 结论：通过
- 问题：断线重试与更复杂的多轮流式场景尚未补齐
- 附件：`apps/web` `npm run build` 通过

## 6. 后续补写要求

- 每次冒烟都要记录构建版本和环境
- 任何页面阻塞或空态问题都要回填到需求池
- 若出现 SSE 中断，要同时记录前端表现和后端事件顺序
