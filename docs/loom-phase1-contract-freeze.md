# loom Phase 1 合同冻结

生效日期：2026-04-08

上游依据：

- `docs/loom-phase1-architecture-design.md`
- `docs/loom-springboot-backend-module-design.md`
- `docs/loom-java-package-structure.md`
- `packages/contracts/src/index.ts`

本文档是 `ARC-001` ~ `ARC-005` 的冻结产物，用于给前端、后端、测试提供同一份可执行基线。

## 1. 冻结范围

本次冻结只覆盖 Phase 1 最小可开发基线：

- 领域对象与状态枚举
- Project / Conversation / Message / Context / Trace / Settings 的 REST 合同
- 会话流式输出的 SSE 事件合同
- 错误口径、时间口径、分页口径
- Phase 1 数据迁移顺序

本次明确排除：

- OpenClaw 真实执行链路
- Feishu 集成
- 外部执行器写路径
- Files / Memory 的完整写接口

## 2. 领域对象冻结

### 2.1 Project

最小字段：

- `id`
- `name`
- `description`
- `status`: `active | archived`
- `instructions`
- `capabilityBindings`
- `pinnedConversationIds`
- `lastMessageAt`
- `updatedAt`

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
- `activeRunId`

### 2.3 Message

最小字段：

- `id`
- `projectId`
- `conversationId`
- `kind`
- `role`
- `body`
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

### 2.4 Context

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

### 2.5 Trace

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

### 2.6 Settings

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

## 3. REST 合同冻结

### 3.1 Envelope

成功响应：

```json
{
  "data": {},
  "meta": {
    "traceId": "trace_123"
  }
}
```

失败响应：

```json
{
  "error": {
    "code": "CONVERSATION_NOT_FOUND",
    "message": "Conversation does not exist"
  },
  "meta": {
    "traceId": "trace_123"
  }
}
```

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

### 3.5 Context

- `GET /api/projects/{projectId}/conversations/{conversationId}/context`
  返回：`ContextPanelView`
- `POST /api/projects/{projectId}/conversations/{conversationId}/context/refresh`
  返回：`ContextRefreshResponse`

### 3.6 Trace

- `GET /api/projects/{projectId}/conversations/{conversationId}/trace`
  返回：`TracePanelView`
- `GET /api/projects/{projectId}/conversations/{conversationId}/runs/{runId}`
  返回：`RunView`

### 3.7 Settings

- `GET /api/settings/overview`
  查询参数：`scope`, `projectId?`, `conversationId?`
  返回：`SettingsOverviewView`

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

## 5. 错误、时间与分页口径

### 5.1 错误口径

错误码采用大写下划线风格，例如：

- `PROJECT_NOT_FOUND`
- `CONVERSATION_NOT_FOUND`
- `MESSAGE_VALIDATION_FAILED`
- `MODEL_PROFILE_NOT_FOUND`
- `STREAM_SUBSCRIPTION_FAILED`

规则：

- `code` 稳定，供前后端和测试断言
- `message` 面向人类阅读，可调整
- 字段级错误放入 `fieldErrors`

### 5.2 时间口径

- 所有时间字段统一使用 ISO 8601 UTC 字符串
- 字段名统一使用 `At` 后缀
- 不在接口层返回“2 分钟前”这类格式化文本

### 5.3 分页口径

- 列表接口统一使用 cursor 分页
- 返回结构统一为 `CursorPage<T>`
- `messages` 默认按 `sequence` 升序
- `conversations`、`projects` 默认按 `updatedAt` 倒序

## 6. 数据迁移顺序冻结

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

- 先保证主链路可跑
- 再补 Trace、Settings、Memory、Files
- 避免前期因次级模块阻塞会话主链路

## 7. Phase 1 边界冻结

- OpenClaw 仅保留可见入口和设置/说明面，不进入 Phase 1 主执行链路
- 前端不得直接调用外部模型或 MCP
- 后端是 Context、Trace、Memory、Run 的唯一权威来源
- `packages/contracts` 是前后端共享类型唯一来源

## 8. 解锁条件

以下条件满足后，后端和前端可以正式开工：

- 本文档与 `packages/contracts/src/index.ts` 合并到集成基线
- 后端按本文档实现接口与事件，不再自定义字段
- 前端按本文档消费字段，不再自行发明读模型
- 测试按本文档生成联调检查单和断言口径
