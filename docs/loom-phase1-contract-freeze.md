# loom Phase 1 合同冻结

生效日期：2026-04-08

上游依据：

- `docs/loom-phase1-architecture-design.md`
- `docs/loom-springboot-backend-module-design.md`
- `docs/loom-java-package-structure.md`
- `packages/contracts/src/index.ts`
- 当前集成分支实现现状（`codex/integration-phase1-delivery`）

本文档是 `ARC-001` ~ `ARC-005` 的冻结产物，用于给前端、后端、测试提供同一份可执行基线。

## 1. 冻结目标

本次冻结覆盖以下内容：

- Phase 1 一等领域对象与状态枚举
- `Project / Conversation / Message / Context / Trace / Settings` 的 REST 合同
- 会话流式输出的 SSE 事件合同
- 错误、时间、分页口径
- 模块边界与跨模块依赖规则
- 数据迁移顺序
- OpenClaw 在 Phase 1 的明确边界

本次明确排除：

- OpenClaw 真实执行链路
- Feishu 集成
- 外部执行器写路径
- Files / Memory 的完整写接口
- 生产环境部署细节

## 2. 领域对象冻结

### 2.1 Project

最小字段：

- `id`
- `name`
- `description`
- `status`: `active | archived`
- `instructions`
- `capabilityBindings.defaultModelProfileId`
- `capabilityBindings.enabledSkillIds`
- `capabilityBindings.defaultRoutingPolicyId`
- `pinnedConversationIds`
- `conversationCount`
- `lastMessageAt`
- `updatedAt`

冻结规则：

- `Project` 是项目级真相源，不允许由前端自行拼装项目绑定关系。
- `Project.status` 在 Phase 1 仅允许 `active` 与 `archived`。

### 2.2 Conversation

最小字段：

- `id`
- `projectId`
- `title`
- `summary`
- `mode`: `chat | plan | action | review`
- `status`: `active | idle | blocked | archived`
- `pinned`
- `lastMessageAt`
- `updatedAt`
- `contextSummary`
- `activeRunId`

冻结规则：

- `Conversation` 是主协作线程，不单独拆出 plan/task 作为上游对象。
- `mode` 是会话内建字段，不允许由前端用页面态替代。

### 2.3 Message

最小字段：

- `id`
- `projectId`
- `conversationId`
- `kind`
- `role`
- `body`
- `summary`
- `statusLabel`
- `sequence`
- `createdAt`
- `completedAt`
- `attachments`

`kind` 冻结为：

- `user`
- `assistant`
- `thinking_summary`
- `action_card`
- `run_progress`
- `external_feedback`
- `context_update`
- `system`

冻结规则：

- `Message.kind` 是渲染合同的一部分，前后端不得增加私有字符串。
- `sequence` 是消息排序真相源；UI 不应依赖“最近更新时间”给消息排序。

### 2.4 Context Snapshot / Context Panel

Context 面板读模型冻结为：

- `conversationSummary`
- `decisions`
- `openLoops`
- `activeGoals`
- `constraints`
- `references`
- `snapshots`
- `updatedAt`

`ContextSnapshot.kind` 冻结为：

- `conversation_summary`
- `decisions`
- `open_loops`
- `planning_state`
- `active_context`

冻结规则：

- 右侧 Context 面板只消费后端组装后的结构化快照，不直接拼接消息历史。
- `references.kind` 只允许 `file | memory | conversation | run`。

### 2.5 Trace / Run / Run Step

Trace 面板读模型冻结为：

- `reasoningSummary`
- `activeRun`
- `steps`
- `updatedAt`

`Run.status` 冻结为：

- `pending`
- `running`
- `waiting`
- `success`
- `failed`
- `cancelled`

`RunStep.status` 冻结为：

- `pending`
- `running`
- `waiting`
- `success`
- `failed`
- `skipped`

冻结规则：

- `Run` 代表一次可见执行实例。
- `RunStep` 是 Trace 面板的最小可视化单位。
- 前端不得自己推导“是否完成”，应以 `status` 为准。

### 2.6 Settings / Capability

Settings 读模型冻结为以下五类：

- `modelProfiles`
- `skills`
- `mcpServers`
- `memoryPolicy`
- `routingPolicy`

作用域冻结为：

- `global`
- `project`
- `conversation`

Phase 1 落地优先级：

- 先实现 `global` 与 `project`
- `conversation` 作为合同保留位

冻结规则：

- `Capabilities` 与 `Settings` 在 Phase 1 共享同一组真相源对象。
- 不允许前端继续完全依赖静态 overview 文本作为真实配置来源。

## 3. REST 合同冻结

### 3.1 Envelope

成功响应：

```json
{
  "data": {},
  "meta": {
    "traceId": "trace_123",
    "requestId": "req_123"
  }
}
```

失败响应：

```json
{
  "data": null,
  "error": {
    "code": "CONVERSATION_NOT_FOUND",
    "message": "Conversation does not exist"
  },
  "meta": {
    "traceId": "trace_123",
    "requestId": "req_123"
  }
}
```

冻结规则：

- 成功响应以 `data` 为主。
- 错误响应以 `error` 为主，允许带 `data: null`。
- `meta` 为可选，但一旦返回必须沿用稳定字段名。

### 3.2 Project

- `GET /api/projects`
  返回：`CursorPage<ProjectListItem>`
- `POST /api/projects`
  请求：`CreateProjectRequest`
  返回：`ProjectView`
- `GET /api/projects/{projectId}`
  返回：`ProjectView`
- `PATCH /api/projects/{projectId}`
  请求：`UpdateProjectRequest`
  返回：`ProjectView`

### 3.3 Conversation

- `GET /api/projects/{projectId}/conversations`
  返回：`CursorPage<ConversationListItem>`
- `POST /api/projects/{projectId}/conversations`
  请求：`CreateConversationRequest`
  返回：`ConversationView`
- `GET /api/projects/{projectId}/conversations/{conversationId}`
  返回：`ConversationView`
- `PATCH /api/projects/{projectId}/conversations/{conversationId}`
  请求：`UpdateConversationRequest`
  返回：`ConversationView`

### 3.4 Message

- `GET /api/projects/{projectId}/conversations/{conversationId}/messages`
  返回：`CursorPage<MessageView>`
- `POST /api/projects/{projectId}/conversations/{conversationId}/messages`
  请求：`SubmitMessageRequest`
  返回：`SubmitMessageResponse`

请求冻结规则：

- `body` 为必填
- `clientMessageId` 为可选去重字段
- `requestedMode` 仅改变本轮处理倾向，不直接替代会话持久化模式
- `attachmentIds`、`allowActions`、`allowMemory` 为可选行为提示

### 3.5 Context

- `GET /api/projects/{projectId}/conversations/{conversationId}/context`
  返回：`ContextPanelView`
- `POST /api/projects/{projectId}/conversations/{conversationId}/context/refresh`
  返回：`ContextRefreshResponse`
- `GET /api/projects/{projectId}/conversations/{conversationId}/context/snapshots`
  返回：`CursorPage<ContextSnapshotView>`
  说明：当前集成分支尚未实现，但属于 ARC 冻结保留接口

### 3.6 Trace

- `GET /api/projects/{projectId}/conversations/{conversationId}/trace`
  返回：`TracePanelView`
- `GET /api/projects/{projectId}/conversations/{conversationId}/runs/{runId}`
  返回：`RunView`
- `GET /api/projects/{projectId}/conversations/{conversationId}/runs/{runId}/steps`
  返回：`CursorPage<RunStepView>`
  说明：当前集成分支尚未实现，但属于 ARC 冻结保留接口

### 3.7 Settings / Capability

- `GET /api/settings/overview`
  查询参数：`scope`, `projectId?`, `conversationId?`
  返回：`SettingsOverviewView`
- `GET /api/capabilities/overview`
  返回：与 `SettingsOverviewView` 同源的 capability summary
  说明：当前集成分支仍通过 bootstrap 提供壳层 overview，后续由 FE/BE lane 接管

## 4. SSE 合同冻结

流入口冻结为：

- `GET /api/projects/{projectId}/conversations/{conversationId}/stream`

事件名冻结为：

- `message.delta`
- `message.done`
- `thinking.summary.delta`
- `thinking.summary.done`
- `trace.step.created`
- `trace.step.updated`
- `trace.step.completed`
- `context.updated`
- `memory.suggested`
- `run.completed`
- `run.failed`

统一字段：

- `event`
- `eventId`
- `projectId`
- `conversationId`
- `emittedAt`

关键事件载荷：

- `message.delta`: `messageId`, `chunk`, `chunkIndex`
- `message.done`: `message`
- `thinking.summary.delta`: `messageId`, `chunk`, `chunkIndex`
- `thinking.summary.done`: `message`
- `trace.step.*`: `runId`, `step`
- `context.updated`: `context`
- `memory.suggested`: `suggestion`
- `run.completed`: `run`
- `run.failed`: `run`, `error`

事件顺序口径：

1. `thinking.summary.delta*`
2. `thinking.summary.done`
3. `message.delta*`
4. `message.done`
5. `trace.step.created/updated/completed`
6. `context.updated`
7. `run.completed | run.failed`

冻结规则：

- 前端只认合同内事件名，不自行发明 `action.*` 私有事件。
- 同一 `conversationId` 的事件必须在单流入口内消费。

## 5. 错误、时间与分页口径

### 5.1 错误口径

错误码采用大写下划线风格，例如：

- `PROJECT_NOT_FOUND`
- `CONVERSATION_NOT_FOUND`
- `REQUEST_VALIDATION_FAILED`
- `MODEL_PROFILE_NOT_FOUND`
- `STREAM_SUBSCRIPTION_FAILED`

规则：

- `code` 稳定，供前后端和测试断言
- `message` 面向人类阅读，可调整
- 字段级错误放入 `fieldErrors`
- `retryable` 只在确需引导重试时返回

### 5.2 时间口径

- 所有时间字段统一使用 ISO 8601 UTC 字符串
- 字段名统一使用 `At` 后缀
- 不在接口层返回“2 分钟前”这类格式化文本
- UI 层负责把 UTC 时间转为相对时间文案

### 5.3 分页口径

- 列表接口统一使用 cursor 分页
- 返回结构统一为 `CursorPage<T>`
- `messages` 默认按 `sequence` 升序
- `runs/steps` 默认按开始时间升序
- `conversations`、`projects` 默认按 `updatedAt` 倒序

## 6. 模块边界冻结

当前仓库实现根包以 `com.loom.server` 为准；逻辑模块边界冻结为：

- `project`
- `conversation`
- `context`
- `memory`
- `action`
- `capability`
- `file`
- `settings`
- `stream`
- `common`

Phase 1 允许的当前过渡实现：

- `workspace` 可以作为过渡聚合模块承接 `project / conversation / context / trace / settings / stream` 的临时组合读模型
- 但后续迁移目标仍应收敛到领域模块，而不是让 `workspace` 长期替代所有模块

跨模块依赖规则：

- `conversation` 可依赖 `context`、`memory`、`action`、`capability`、`stream`
- `context` 可依赖 `project`、`memory`、`file`
- `memory` 不直接依赖 `conversation` 仓储
- `action` 可依赖 `capability`、`stream`
- `settings` 不依赖 `conversation`
- `stream` 只依赖发布出的事件 DTO，不拥有业务编排逻辑

## 7. 数据迁移顺序冻结

首批迁移顺序冻结为：

1. `project`
2. `conversation`
3. `message`
4. `context_snapshot`
5. `action`
6. `run`
7. `run_step`
8. `artifact`
9. `model_profile`
10. `mcp_server`
11. `skill`
12. `setting_memory_policy`
13. `setting_routing_policy`
14. `memory_item`
15. `memory_suggestion`
16. `file_asset`
17. `message_asset_ref`

理由：

- 先保证会话主链路可跑
- 再补 Trace、Settings、Memory、Files
- 避免因次级模块阻塞会话、上下文和流式主路径

## 8. OpenClaw 边界冻结

Phase 1 中 OpenClaw 的边界冻结如下：

- 可以保留可见入口、说明页、状态页和配置位
- 可以保留 `routingPolicy.externalExecutorLabel` 等扩展槽位
- 不进入 `SubmitMessageRequest -> Run -> Stream` 的主执行链路
- 不承担 Context、Trace、Memory 的真相源职责
- 不作为前端直连目标

结论：

- loom Phase 1 必须先完成“内部会话产品”闭环
- OpenClaw 仅作为 Phase 2 准备位存在

## 9. 与当前实现的对齐结论

截至当前集成分支：

- `BE-002` 已落地 `project / conversation / message / context / trace / settings / stream` 的最小接口骨架
- `BE-003` 已落地合同内 SSE 事件名
- `FE-002`、`FE-003` 已开始消费这些接口与事件

当前仍待补齐的实现项：

- `context snapshots` 独立查询
- `run steps` 独立列表查询
- `settings / capability` 页面从壳层 bootstrap 完全迁移到真实读模型
- `memory.suggested` 的真实写入链路

## 10. 解锁条件

以下条件满足后，后端和前端可以正式以本合同为唯一依据继续推进：

- 本文档与 `packages/contracts/src/index.ts` 合并到集成基线
- 后端按本文档实现接口与事件，不再自定义字段
- 前端按本文档消费字段，不再自行发明读模型
- 测试按本文档生成联调检查单和断言口径
