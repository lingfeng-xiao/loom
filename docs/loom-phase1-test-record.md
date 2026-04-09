# loom Phase 1 测试记录

生效日期：2026-04-08

本文件用于沉淀 Phase 1 的每次测试、联调、生产候选验证记录。

## 1. 记录原则

- 一次测试只记录一个清晰范围
- 结论必须可回看、可复核、可回退
- 任何阻塞项都要写明责任方和下一步动作
- 测试结论必须与需求 ID 绑定

## 2. 记录条目

| 字段 | 内容 |
| --- | --- |
| 记录 ID | 例如 `TR-2026-04-08-01` |
| 日期 | YYYY-MM-DD |
| 阶段 | 基线 / 合同冻结 / 并发开发 / 联调 / 生产验证 |
| 覆盖需求 | 本次覆盖的需求 ID 列表 |
| 环境 | 本地 / 集成分支 / 生产候选 / 生产 |
| 执行人 | QA / 前端 / 后端 / PM |
| 结论 | 通过 / 部分通过 / 阻塞 |
| 问题 | 具体问题与影响范围 |
| 修复 | 修复人、修复分支、修复 PR |
| 回退 | 是否执行回退、回退是否成功 |
| 附件 | 截图、录屏、日志、接口样例、事件样例 |

## 3. 推荐记录格式

### 3.1 基线记录

- `T-BASE-01`
- `T-BASE-02`
- contracts 编译记录

### 3.2 联调记录

- 接口字段对齐结果
- SSE 事件顺序结果
- 前端页面行为结果
- 异常和空态结果

### 3.3 生产验证记录

- release 版本号或 commit
- smoke 结果
- 回退命令执行情况
- 最终结论

## 4. 回填要求

每次测试结束后，至少回填以下内容：

- 结论
- 问题
- 下一步动作
- 是否允许进入下一阶段

## 5. 最新记录

### TR-2026-04-08-01

- 日期：2026-04-08
- 阶段：并发开发
- 覆盖需求：`BE-001`、`BE-002`、`QA-002`
- 环境：本地 / `codex/integration-phase1-delivery`
- 执行人：后端 / PM
- 结论：通过
- 问题：未发现阻塞当前提交的后端测试问题；`service` 健康探针仍保留 `template-server` 文案，暂未纳入本轮改名范围。
- 修复：后端已提交 `687d57d` `BE-002 feat: add phase1 workspace api`
- 回退：未执行
- 附件：`apps/server` 下执行 `./mvnw -q test` 通过；覆盖 `/api/projects`、`/conversations`、`/messages`、`/context`、`/trace`、`/settings/overview`、`/stream`
- 下一步动作：继续补 `BE-003` 的 SSE 事件细化与 `QA-002` 的错误态回归
- 是否允许进入下一阶段：允许继续推进前端接线

### TR-2026-04-08-02

- 日期：2026-04-08
- 阶段：并发开发
- 覆盖需求：`FE-001`、`FE-002`、`QA-003`
- 环境：本地 / `codex/integration-phase1-delivery`
- 执行人：前端 / PM
- 结论：通过
- 问题：当前前端仍以 bootstrap 负载为主视图模型，消息提交后通过刷新 bootstrap 回填最新状态，尚未进入 SSE 实时渲染阶段。
- 修复：当前工作区已补齐 composer 提交消息、bootstrap 手动刷新和提交中状态透传，待前端提交落盘
- 回退：未执行
- 附件：`apps/web` 下执行 `npm run build` 通过
- 下一步动作：把前端接线提交为独立需求提交，并继续推进 `FE-003`
- 是否允许进入下一阶段：允许继续推进联调前置开发

### TR-2026-04-08-03

- 日期：2026-04-08
- 阶段：并发开发
- 覆盖需求：`BE-003`、`QA-002`
- 环境：本地 / `codex/integration-phase1-delivery`
- 执行人：后端 / PM
- 结论：通过
- 问题：当前 SSE 仍以单次提交后的短连接事件快照为主，尚未接入真正长时运行流。
- 修复：已提交 `0fdb1e9` `BE-003 feat: expand workspace stream events`
- 回退：未执行
- 附件：`apps/server` 下执行 `./mvnw -q test` 通过；新增断言覆盖 `thinking.summary.delta`、`thinking.summary.done`、`message.delta`、`message.done`、`trace.step.created`、`trace.step.completed`、`context.updated`、`run.completed`
- 下一步动作：继续把 FE 端实时渲染与 SSE 断线处理补齐
- 是否允许进入下一阶段：允许继续推进 FE-003

### TR-2026-04-08-04

- 日期：2026-04-08
- 阶段：并发开发
- 覆盖需求：`FE-003`、`QA-003`
- 环境：本地 / `codex/integration-phase1-delivery`
- 执行人：前端 / PM
- 结论：通过
- 问题：当前前端通过 EventSource 消费短连接流事件，并在流关闭后刷新 bootstrap；尚未补齐断线重试和手动联调证据。
- 修复：已提交 `d5a748f` `FE-003 feat: apply workspace stream events in conversation view`
- 回退：未执行
- 附件：`apps/web` 下执行 `npm run build` 通过
- 下一步动作：补联调清单执行记录，并推进 `FE-004 / BE-004`
- 是否允许进入下一阶段：允许继续推进 Context 真数据接线

### TR-2026-04-08-05

- 日期：2026-04-08
- 阶段：并发开发
- 覆盖需求：`BE-004`、`BE-005`
- 环境：本地 / `codex/integration-phase1-delivery`
- 执行人：后端 / PM
- 结论：通过
- 问题：当前 capability overview 仍由 `workspace` 过渡聚合模块装配，后续可继续下沉到独立 capability 模块。
- 修复：已提交 `13143aa` `BE-004 feat: add context refresh and capability overview`
- 回退：未执行
- 附件：`apps/server` 下执行 `./mvnw -q test` 通过；测试覆盖 `POST /context/refresh` 与 `GET /api/capabilities/overview`
- 下一步动作：继续推进 `BE-006` 或拆出独立 capability / action 模块
- 是否允许进入下一阶段：允许进入前端真数据接线与联调阶段

### TR-2026-04-08-06

- 日期：2026-04-08
- 阶段：并发开发
- 覆盖需求：`FE-004`、`FE-005`
- 环境：本地 / `codex/integration-phase1-delivery`
- 执行人：前端 / PM
- 结论：通过
- 问题：Capabilities / Settings 目前通过 provider 层的远端读模型覆盖现有 payload，仍需补页面级联调证据确认展示口径。
- 修复：已提交 `44c6c1f` `FE-004 feat: source context and settings panels from workspace apis`
- 回退：未执行
- 附件：`apps/web` 下执行 `npm run build` 通过
- 下一步动作：补 `FE-005` 联调证据并推进 `FE-006`
- 是否允许进入下一阶段：允许继续推进文件池与内存页首版

### TR-2026-04-08-07

- 日期：2026-04-08
- 阶段：并发开发
- 覆盖需求：`BE-006`、`BE-007`
- 环境：本地 / `codex/integration-phase1-delivery`
- 执行人：后端 / PM
- 结论：通过
- 问题：`action` 的显式建模仍未拆出，当前 `run steps` 查询仍由 workspace 过渡聚合模块提供。
- 修复：已提交 `ebbc1a6` `BE-006 feat: add run steps and workspace asset feeds`
- 回退：未执行
- 附件：`apps/server` 下执行 `./mvnw -q test` 通过；新增覆盖 `GET /runs/{runId}/steps`、`GET /files`、`GET /memory`
- 下一步动作：视验收需要决定是否继续显式拆出 action / run 模块
- 是否允许进入下一阶段：允许 Files / Memory 前端页面接线

### TR-2026-04-08-08

- 日期：2026-04-08
- 阶段：并发开发
- 覆盖需求：`FE-006`
- 环境：本地 / `codex/integration-phase1-delivery`
- 执行人：前端 / PM
- 结论：通过
- 问题：Files / Memory 目前仍以 page-local 读取逻辑接 API，后续若范围继续扩大，可再收敛进统一 provider 层。
- 修复：已提交 `1a92569` `FE-006 feat: add files and memory workspace pages`
- 回退：未执行
- 附件：`apps/web` 下执行 `npm run build` 通过
- 下一步动作：补 `FE-005` 页面级联调证据
- 是否允许进入下一阶段：允许进入最终联调与验收收口

### TR-2026-04-08-09

- 日期：2026-04-08
- 阶段：联调
- 覆盖需求：`BE-006`、`FE-005`、`QA-002`、`QA-004`、`QA-005`
- 环境：本地 / `codex/integration-phase1-delivery`
- 执行人：PM
- 结论：通过
- 问题：当前验收范围已具备本地与集成分支验收条件，但生产机只读检查窗口仍未开启，因此本次结论不覆盖生产验证。
- 修复：后端补齐 action 查询读模型与测试；前端补齐 project / conversation / message / trace / settings / capabilities 的远端读取链路；contracts 与联调文档已同步回填。
- 回退：未执行
- 附件：`apps/server` 下执行 `./mvnw.cmd -q test` 通过；`apps/web` 下执行 `npm run build` 通过；仓库根目录执行 `npx -p typescript@5.6.3 tsc -p packages/contracts/tsconfig.json --noEmit` 通过
- 下一步动作：进入产品/研发验收；若需进入生产验证，先执行 `ssh jd` 只读检查窗口并补 smoke 记录
- 是否允许进入下一阶段：允许进入验收，不允许直接进入生产机写操作

### TR-2026-04-08-10

- 日期：2026-04-08
- 阶段：发布前本地验收
- 覆盖需求：`FE-005`、`BE-006`、`QA-005`
- 环境：本地 acceptance 部署
- 执行人：PM
- 结论：通过
- 问题：当前仅验证本地 acceptance 地址与本地 API smoke，尚未执行生产机只读检查、候选部署或回退演练。
- 修复：无新增修复，本次主要验证当前集成结果可作为验收入口使用。
- 回退：未执行
- 附件：前端验收地址 `http://127.0.0.1:4173` 返回 `200`；`http://127.0.0.1:4173/api/health` 通过代理返回 `ok`；后端 `http://127.0.0.1:8080/api/projects` 返回项目列表
- 下一步动作：如需进入发布候选，先补生产机 smoke 与回退记录
- 是否允许进入下一阶段：允许作为本地验收地址交付

## 6. 证据回填摘要

### 后端证据

- `BE-002`
  - 提交：`687d57d`
  - 验证：`apps/server` 执行 `./mvnw -q test` 通过
  - 覆盖：Project / Conversation / Message / Context / Trace / Settings / Stream 最小接口
  - 结论：接口层可作为前后端接线基线
- `BE-003`
  - 提交：`0fdb1e9`
  - 验证：集成测试新增 SSE 事件断言，事件名已锁定
  - 覆盖：`thinking.summary.delta`、`thinking.summary.done`、`message.delta`、`message.done`、`trace.step.created`、`trace.step.completed`、`context.updated`、`run.completed`
  - 结论：流式层可作为前端订阅基线

### 前端证据

- `FE-002`
  - 提交：`80a5084`
  - 验证：消息提交接通真实 workspace API，提交后触发 bootstrap 刷新
  - 结论：composer 到后端主数据链路已打通
- `FE-003`
  - 提交：`d5a748f`
  - 验证：`apps/web` 执行 `npm run build` 通过
  - 覆盖：EventSource 事件消费、消息 / Trace / Context 视图回填
  - 结论：前端已具备最小实时更新能力

### 当前缺口

- `FE-001 / FE-002 / FE-003` 仍处于“真实接口已接线但体验与降级策略仍可继续收口”的阶段
- `BE-001 / BE-002 / BE-003` 仍存在 template 命名残留、短连接 SSE 与过渡聚合模块等产品化缺口
- `QA-003` 仍缺更完整的前端关键路径自动化或手测截图/录屏证据
- 本轮结论仅覆盖本地与集成分支验收，不覆盖生产机 smoke 与回退演练
