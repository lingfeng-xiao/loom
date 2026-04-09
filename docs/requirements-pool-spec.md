# loom 需求池

> Conversation closure wave upstream plan: `docs/loom-conversation-closure-plan.md`

生效日期：2026-04-09

设计来源：
- `docs/loom-conversation-closure-plan.md`
- `docs/loom-phase1-architecture-design.md`
- `docs/loom-springboot-backend-module-design.md`
- `docs/loom-java-package-structure.md`
- `docs/development-spec.md`
- 当前仓库实现现状（`apps/web`、`apps/server`、`packages/contracts`）

## 0. Conversation Closure Wave (2026-04-09)

### 0.1 Maintenance rules for this wave

- 本波次唯一上游总计划文档为 `docs/loom-conversation-closure-plan.md`。
- 本波次需求按 `ARC / FE / BE / PM / QA / OPS` 角色拆分维护。
- 每条需求都必须显式维护 `Source`、`Dependency`、`Acceptance`。
- 只有当主计划文档已落地并被需求池引用后，实现侧需求才能从 `Ready` 进入 `In Progress`。
- 删除类改动必须在需求池中可追踪，不允许先删代码再补文档。

### 0.2 Architecture / ARC

| Requirement ID | Priority | Status | Title | Source | Dependency | Acceptance |
| --- | --- | --- | --- | --- | --- | --- |
| ARC-007 | P0 | Ready | Freeze the conversation-closure product boundary and keep only project, conversation, context, memory, stream, and the minimum deployment/ops baseline. | `docs/loom-conversation-closure-plan.md` | None | The active scope explicitly keeps only the product-grade conversation surface, and removed concepts/modules are listed for deletion or archive. |
| ARC-008 | P0 | Ready | Freeze the target architecture and module dependency rules for `common / project / conversation / context / memory / stream`. | `docs/loom-conversation-closure-plan.md` | ARC-007 | Backend and frontend target structure, domain ownership, and dependency boundaries are documented and accepted as the only current architecture baseline. |
| ARC-009 | P0 | Ready | Freeze the standards set and require each standard to map to concrete system constraints. | `docs/loom-conversation-closure-plan.md` | ARC-007, ARC-008 | Frontend, backend, git, Maven, release, file, contract, test, doc, config, and logging standards are written down with explicit codebase/process mapping. |
| ARC-010 | P0 | Ready | Freeze document cleanup, archive policy, and current-valid document boundaries for this wave. | `docs/loom-conversation-closure-plan.md` | ARC-007 | Current-valid docs, archive treatment, and deletion criteria are explicit, and no expired doc remains mixed into the active baseline by accident. |

### 0.3 Frontend / FE

| Requirement ID | Priority | Status | Title | Source | Dependency | Acceptance |
| --- | --- | --- | --- | --- | --- | --- |
| FE-010 | P0 | Ready | Remove mode concepts, data-source concepts, and non-core pages/entry points from the frontend product surface. | `docs/loom-conversation-closure-plan.md` | ARC-007, ARC-010 | The UI no longer exposes `Chat / Plan / Action / Review`, local/remote/fallback data-source labels, or routes/pages outside the conversation-closure scope. |
| FE-011 | P0 | Ready | Refactor frontend business state into `project / conversation / context / memory / session-ui` domain stores. | `docs/loom-conversation-closure-plan.md` | ARC-008, FE-010, BE-012 | The frontend no longer depends on a single business-truth provider, and each retained domain owns its own state boundary. |
| FE-012 | P0 | Ready | Converge the conversation, context, and memory surfaces into the minimum product-grade workspace. | `docs/loom-conversation-closure-plan.md` | FE-010, FE-011, BE-015 | Users can complete the retained conversation flow without seeing removed concepts, dead entry points, or placeholder shells. |
| FE-013 | P0 | Ready | Clean obsolete frontend types, services, routes, styles, and seed/demo data tied to removed concepts or modules. | `docs/loom-conversation-closure-plan.md` | FE-010, FE-011, ARC-010 | Removed concepts have no primary-path residue in frontend code, docs, routes, demo data, or build inputs. |

### 0.4 Backend / BE

| Requirement ID | Priority | Status | Title | Source | Dependency | Acceptance |
| --- | --- | --- | --- | --- | --- | --- |
| BE-012 | P0 | Ready | Converge backend modules to `project / conversation / context / memory / stream / common` and demote/remove the old workspace-centered aggregate. | `docs/loom-conversation-closure-plan.md` | ARC-007, ARC-008 | Module ownership is explicit, cross-module repository access is eliminated, and the retained backend surface matches the closure architecture. |
| BE-013 | P0 | Ready | Persist conversation, message, action/run state and remove in-memory business truth from the workspace aggregate. | `docs/loom-conversation-closure-plan.md` | ARC-008, BE-012 | Conversation, message, context, memory, and run state are restart-safe and no longer depend on long-lived in-memory business truth. |
| BE-014 | P0 | Ready | Remove mode, data-source, and obsolete workspace-shell semantics from backend contracts and APIs. | `docs/loom-conversation-closure-plan.md` | ARC-007, BE-012 | Backend contracts no longer expose removed concepts, and deleted semantics do not remain as compatibility leftovers in active APIs. |
| BE-015 | P0 | Ready | Productize the conversation, context, memory, stream, and recovery closure path. | `docs/loom-conversation-closure-plan.md` | BE-013, BE-014, QA-008 | The backend supports a stable end-to-end flow for send, stream, context refresh, memory suggestion handling, page refresh recovery, and service restart recovery. |

### 0.5 PM

| Requirement ID | Priority | Status | Title | Source | Dependency | Acceptance |
| --- | --- | --- | --- | --- | --- | --- |
| PM-007 | P0 | Ready | Adopt the conversation closure plan as the single upstream plan and align the requirement pool to it. | `docs/loom-conversation-closure-plan.md` | ARC-007, ARC-010 | The plan document is referenced by docs index/readme and by every new wave requirement that belongs to this closure effort. |
| PM-008 | P0 | Ready | Maintain the current-valid, archive, and deletion document lists for this wave. | `docs/loom-conversation-closure-plan.md` | PM-007, ARC-010 | There is a maintained list of active docs, archive candidates, and deletion candidates, and the current-valid set stays clean throughout the wave. |
| PM-009 | P0 | Ready | Maintain wave sequencing, dependency gates, acceptance exit, and go/no-go rules for the closure wave. | `docs/loom-conversation-closure-plan.md` | PM-007, ARC-008, QA-008, OPS-003 | Stage gates, dependency locks, and exit criteria are visible and kept current so implementation does not outrun frozen boundaries. |

### 0.6 QA

| Requirement ID | Priority | Status | Title | Source | Dependency | Acceptance |
| --- | --- | --- | --- | --- | --- | --- |
| QA-008 | P0 | Ready | Establish the product-grade conversation-closure acceptance matrix. | `docs/loom-conversation-closure-plan.md` | ARC-007, ARC-008, BE-015, FE-012 | Acceptance covers project -> conversation -> send -> stream -> context -> memory suggestion -> refresh -> restart recovery as a single retained user flow. |
| QA-009 | P0 | Ready | Establish the cleanliness regression checklist for removed concepts, pages, routes, docs, configs, and seeds. | `docs/loom-conversation-closure-plan.md` | ARC-010, FE-013, BE-014, PM-008 | Regression checks can prove that removed concepts and their technical residue are no longer present in primary product and delivery paths. |
| QA-010 | P0 | Ready | Cover stream, recovery, context, memory, and removed-concept regressions across backend, frontend, and deployment smoke. | `docs/loom-conversation-closure-plan.md` | QA-008, QA-009, FE-012, BE-015, OPS-003 | Validation captures successful and failing stream flows, refresh/restart recovery, context and memory behavior, and non-regression after concept cleanup. |

### 0.7 Ops / OPS

| Requirement ID | Priority | Status | Title | Source | Dependency | Acceptance |
| --- | --- | --- | --- | --- | --- | --- |
| OPS-002 | P0 | Ready | Converge deployment scripts, env vars, naming, image/runtime labels, and operational terminology to the conversation-closure scope. | `docs/loom-conversation-closure-plan.md` | ARC-010, BE-012 | Deployment/runtime artifacts no longer reference removed modules or stale naming, and the retained deployment surface matches the current product boundary. |
| OPS-003 | P0 | Ready | Freeze the minimum `preflight / smoke / promote / rollback` operational closure for the retained product. | `docs/loom-conversation-closure-plan.md` | OPS-002, QA-008 | A minimum but real release path exists for the retained product surface, including smoke and rollback readiness for the closure scope. |
| OPS-004 | P0 | Ready | Clean deployment and runtime leftovers tied to removed modules, pages, concepts, and historical naming. | `docs/loom-conversation-closure-plan.md` | OPS-002, ARC-010 | Compose/env/script/runtime leftovers that belong to removed scope are deleted or archived and no longer appear in active deployment instructions. |

## 1. 当前项目基线

### 1.1 已有能力

- Web 端已经完成 loom Phase 1 壳层：三栏布局、欢迎页、会话页、Capabilities、OpenClaw、Settings 页面可运行，`npm run build` 已通过。
- Web 端当前主要依赖 bootstrap 读取模型和本地 fallback 数据，Files / Memory 仍是占位入口。
- Server 端已有 `bootstrap`、`settings`、`health`、`nodes` 基础接口，`./mvnw -q test` 已通过。
- `packages/contracts` 已抽出一份供前后端共享的 loom shell bootstrap 合同，但包名和部分命名仍保留 template 痕迹。

### 1.2 关键缺口

- 后端尚未进入 Phase 1 模块化单体实现，`project / conversation / context / memory / action / capability / file / stream` 等模块仍未落地。
- 前端会话工作台还没有接入真实项目、会话、消息、上下文、Trace、Settings 数据流。
- 契约目前只覆盖壳层 bootstrap，未冻结真实读写接口、SSE 事件模型和错误口径。
- 文档目录此前已清空旧需求池，当前需要基于新设计重新建立角色化需求维护入口。

## 2. 维护规则

每条需求至少维护以下字段：

| 字段 | 说明 |
| --- | --- |
| 需求 ID | 唯一编号，格式 `ROLE-序号` |
| 角色 | `webAI 架构师` / `前端开发` / `后端开发` / `经理` / `测试` |
| 优先级 | `P0`、`P1`、`P2` |
| 状态 | `Ready`、`In Progress`、`Blocked`、`Done` |
| 来源 | 设计文档或当前实现缺口 |
| 依赖 | 上游需求 ID 或文档冻结项 |
| 验收标准 | 可直接用于评审、联调或测试的结果 |

状态口径：

- `Ready`：文档和边界足够明确，可以开始执行。
- `In Progress`：代码或文档已开始推进，但还未闭环。
- `Blocked`：依赖未冻结，不能直接开发。
- `Done`：实现、文档、验证均已完成。

## 3. 需求池

### 3.1 webAI 架构师

| 需求 ID | 优先级 | 状态 | 需求项 | 来源 | 依赖 | 验收标准 |
| --- | --- | --- | --- | --- | --- | --- |
| ARC-001 | P0 | Done | 冻结 Phase 1 领域边界，明确 Project、Conversation、Message、Context Snapshot、Memory、Action、Run、File Asset 的最小定义与状态枚举。 | 架构设计、第 6/8/9/12/17 节 | 无 | 输出统一术语、对象定义、状态口径，供前后端和测试直接引用。 |
| ARC-002 | P0 | Done | 冻结前后端最小合同：bootstrap 之后新增 project、conversation、message、context、trace、settings 的 REST 结构和 SSE 事件结构。 | 架构设计、第 16/18 节；后端模块设计、第 8/10/11 节 | ARC-001 | 形成 API / event 契约基线，包含字段、错误口径、时间字段和分页约定。 |
| ARC-003 | P0 | Done | 冻结 Phase 1 模块化单体边界和包结构迁移策略，确认从 `com.template.server` 向 loom 领域包迁移的顺序。 | 后端模块设计、第 3/5/7 节；Java 包结构文档 | ARC-001 | 产出模块边界和迁移顺序，禁止跨模块直连仓储。 |
| ARC-004 | P1 | Done | 冻结内部执行与 OpenClaw 的阶段边界，明确 Phase 1 中哪些能力保留可见入口、哪些不进入真实执行链路。 | 架构设计、第 4/13/20/21 节 | ARC-002 | 输出一份范围说明，避免前后端把 OpenClaw 误接入主链路。 |
| ARC-005 | P1 | Done | 制定 Phase 1 数据模型和迁移清单，覆盖 `project` 到 `run_step` 的首批表结构。 | 架构设计、第 17 节；后端模块设计、第 9/12 节 | ARC-001, ARC-003 | 表结构、迁移顺序、主外键关系和审计字段口径明确。 |

### 3.2 前端开发

| 需求 ID | 优先级 | 状态 | 需求项 | 来源 | 依赖 | 验收标准 |
| --- | --- | --- | --- | --- | --- | --- |
| FE-001 | P0 | In Progress | 将现有三栏壳层从纯 fallback/bootstrap 展示升级为“可切换真实数据源”的工作台，保留加载态、错态和空态。 | 当前 `apps/web` 实现现状 | ARC-002 | 本地无后端时可回退，有后端时优先读真实接口，状态表现完整。 |
| FE-002 | P0 | In Progress | 接入项目与会话基础能力：项目摘要、会话列表、消息列表、模式切换、草稿输入。 | 架构设计、第 7/8/18 节 | ARC-002, BE-002 | 会话页不再只消费静态 bootstrap，主链路基于真实接口渲染。 |
| FE-003 | P0 | In Progress | 接入消息流式输出与 Trace 面板实时更新，完成 message stream、thinking summary、run step 的前端订阅与渲染。 | 架构设计、第 14/18 节；后端模块设计、第 10 节 | ARC-002, BE-003 | 会话回复、思考摘要、Trace 状态变化通过统一 SSE 合同实时更新。 |
| FE-004 | P1 | Done | 将 Context 右侧面板升级为真实数据面板，展示目标、约束、摘要、引用文件、未闭环事项。 | 架构设计、第 7/10 节 | ARC-002, BE-004 | Context 面板支持读真实上下文快照，字段命名和排序一致。 |
| FE-005 | P1 | Done | 将 Capabilities 与 Settings 页面改为真实读模型，并支持分区切换、配置摘要、风险提示。 | 架构设计、第 13/15 节 | ARC-002, BE-005 | 页面数据来自服务端，不再完全依赖静态 overview。 |
| FE-006 | P2 | Done | 落地 Files / Memory 页面首版壳层，支持列表、空态、入口联动，为后续 Phase 1.5 做准备。 | 架构设计、第 9/11 节 | ARC-002, BE-006 | Files / Memory 不再是纯占位页，具备最小浏览能力。 |

### 3.3 后端开发

| 需求 ID | 优先级 | 状态 | 需求项 | 来源 | 依赖 | 验收标准 |
| --- | --- | --- | --- | --- | --- | --- |
| BE-001 | P0 | In Progress | 完成服务端命名与骨架迁移：从 template 命名迁到 loom 领域命名，并建立模块化包结构。 | 当前 `apps/server` 现状；Java 包结构文档 | ARC-003 | 根包、配置、启动类、合同命名对齐 loom，迁移后测试仍可运行。 |
| BE-002 | P0 | In Progress | 实现 Project / Conversation / Message 最小可用读写接口，形成“提交消息前”的真实主数据基础。 | 架构设计、第 6/16/18/19 节 | ARC-001, ARC-002, ARC-005 | 至少支持项目详情、会话列表、消息列表、创建会话、提交用户消息。 |
| BE-003 | P0 | In Progress | 实现 stream 模块和 SSE 事件合同，支持 `message.delta`、`message.done`、`thinking.summary.*`、`trace.step.*` 等事件。 | 后端模块设计、第 9/10 节 | ARC-002, BE-002 | 前端可稳定订阅单一流入口，断开重连和完成态口径一致。 |
| BE-004 | P1 | Done | 实现 context 模块首版：上下文组装、摘要刷新、决策与 open loops 快照查询。 | 架构设计、第 10/18 节；后端模块设计、第 4.3 节 | ARC-001, ARC-002, ARC-005 | 提供 Context 面板读接口和刷新接口，结构与文档一致。 |
| BE-005 | P1 | Done | 实现 settings / capability 最小读接口，包含 model profiles、skills、MCP、routing、memory policy 的读取视图。 | 架构设计、第 13/15 节；后端模块设计、第 4.6/4.8 节 | ARC-002, ARC-005 | Capabilities / Settings 页面可读取真实摘要数据。 |
| BE-006 | P1 | Done | 实现 action / trace 最小链路，支持创建 action、run、run_step 并对外查询。 | 架构设计、第 12/14 节；后端模块设计、第 4.5 节 | ARC-001, ARC-002, BE-003 | Trace 面板有真实 run step 数据来源，失败和等待态可区分。 |
| BE-007 | P2 | Done | 实现 file / memory 首版数据模块，支持项目文件元数据与分层 memory 列表。 | 架构设计、第 9/11 节 | ARC-001, ARC-002, ARC-005 | Files / Memory 页面具备真实列表读取能力。 |

### 3.4 经理

| 需求 ID | 优先级 | 状态 | 需求项 | 来源 | 依赖 | 验收标准 |
| --- | --- | --- | --- | --- | --- | --- |
| PM-001 | P0 | In Progress | 以新架构文档为唯一上游，重建 Phase 1 里程碑、角色 owner、依赖关系和排期。 | 开发规范、第 3/4 节 | ARC-001, ARC-002, ARC-003 | 每条需求都有 owner、优先级、计划阶段和阻塞记录。 |
| PM-002 | P0 | Ready | 建立需求变更控制：任何新增或变更需求必须回填到本需求池，不允许口头漂移。 | 开发规范、第 10 节 | 无 | 需求池成为唯一任务入口，PR 必须关联需求 ID。 |
| PM-003 | P0 | In Progress | 组织契约冻结节奏，先完成架构/合同基线，再安排前后端并行开发和联调窗口。 | 开发规范、第 4.2/4.3/4.5 节 | ARC-002 | 前后端开始并行前，接口、事件、错误口径已冻结。 |
| PM-004 | P1 | In Progress | 建立 Phase 1 风险台账，重点跟踪 template 命名迁移、SSE 稳定性、OpenClaw 范围漂移和测试基线缺失。 | 当前项目现状 | PM-001 | 风险具备负责人、触发条件、缓解措施和升级路径。 |
| PM-005 | P1 | Ready | 组织验收节奏，定义“壳层完成”“真实主链路完成”“Trace 可见完成”三个阶段验收门槛。 | 架构设计、第 19 节；开发规范、第 11 节 | PM-001, QA-001 | 阶段出口条件可执行，避免以视觉完成替代业务完成。 |

### 3.5 测试

| 需求 ID | 优先级 | 状态 | 需求项 | 来源 | 依赖 | 验收标准 |
| --- | --- | --- | --- | --- | --- | --- |
| QA-001 | P0 | Done | 基于新架构补 Phase 1 测试计划，覆盖会话主链路、Trace、Context、Settings、降级策略。 | 开发规范、第 9 节；当前测试文档缺位 | ARC-002, PM-001 | 输出测试范围、环境、入口数据、回归清单和发布建议口径。 |
| QA-002 | P0 | Done | 扩展后端集成测试，覆盖 project / conversation / message / stream / settings 新接口。 | 当前 `apps/server` 测试现状 | BE-002, BE-003, BE-005 | 新接口具备 MockMvc 或集成测试，关键错误态可回归。 |
| QA-003 | P0 | In Progress | 建立前端冒烟与关键路径验证，覆盖欢迎页进入、会话切换、模式切换、流式回复、右侧面板切换。 | 当前 `apps/web` 实现现状 | FE-002, FE-003, FE-004 | 主链路有自动化或明确手测清单，可在每次合并前执行。 |
| QA-004 | P1 | Done | 建立前后端联调检查单，明确契约字段、事件顺序、断线重连、空态和错态。 | 开发规范、第 4.5 节 | ARC-002, FE-003, BE-003 | 联调结果可回填到需求条目，不再只留口头结论。 |
| QA-005 | P1 | Done | 建立发布前验收与回归建议，区分 P0 主链路、P1 配置面、P2 占位能力。 | 开发规范、第 4.6/4.7 节 | PM-005 | 发布前有一份明确的 go / no-go 检查清单。 |

## 4. 当前执行顺序建议

1. 先完成 `ARC-001`、`ARC-002`、`ARC-003`，冻结领域边界、合同和迁移路线。
2. 并行推进 `BE-001`、`BE-002`、`FE-001`，把前端壳层和后端真实主数据接起来。
3. 再推进 `BE-003`、`BE-004`、`BE-005` 与 `FE-003`、`FE-004`、`FE-005`，补齐流式、Context、Trace、Settings。
4. `BE-006`、`FE-006`、`QA-004`、`QA-005` 作为 Phase 1 后半段收口项推进。

## 5. 维护说明

- 本文档从 2026-04-08 起重新启用，旧需求池内容不再恢复。
- 后续新增需求必须以本需求池为唯一入口，并同步回填状态、阻塞项、文档影响和测试结果。
- 如果设计边界发生变化，先更新架构文档和本需求池，再进入实现。

## 6. PM 进展回填（2026-04-08）

- `BE-001`：已在 `codex/backend-be-core-phase1` 形成独立提交 `282a2f8`，完成 loom 根包迁移与基础测试回归，状态维持 `In Progress`，等待架构合同冻结后一起验收。
- `BE-002`：已在 `codex/integration-phase1-delivery` 提交 `687d57d`，补齐 `project / conversation / message / context / trace / settings / stream` 的 Phase 1 最小接口与错误口径，当前进入 `In Progress`。
- `BE-003`：已在 `codex/integration-phase1-delivery` 提交 `0fdb1e9`，SSE 事件补齐 `thinking.summary.*`、`message.*`、`trace.step.*`、`context.updated`、`run.completed`，并用集成测试锁定事件名。
- `BE-004`：已在 `codex/integration-phase1-delivery` 提交 `13143aa`，Context 读模型改为真实会话上下文状态，`context/refresh` 具备可回归的刷新行为与快照更新，状态更新为 `Done`。
- `BE-005`：已通过 `13143aa` 一并补齐 `/api/capabilities/overview`，形成 Settings / Capabilities 的最小真实读接口，状态更新为 `Done`。
- `BE-006`：在 `ebbc1a6` 的 `run steps` 基础上继续补齐 action 读模型与查询接口，`TracePanelView` 已显式暴露 `activeAction`，并由集成测试覆盖 action 成功查询与 404 错误态，状态更新为 `Done`。
- `BE-007`：已通过 `ebbc1a6` 一并补齐 `/api/projects/{projectId}/files` 与 `/api/projects/{projectId}/memory`，状态更新为 `Done`。
- `ARC-001` ~ `ARC-005`：已通过 `de52aa5` 与 `82877a7` 形成领域、合同、模块边界、迁移顺序与 OpenClaw 范围冻结基线，状态更新为 `Done`。
- `FE-001`：保留远端 / fallback 双源切换能力，并保持构建通过，继续作为前端主链路接线基线。
- `FE-002`：当前已接通 composer 提交消息到后端并触发 bootstrap 刷新，状态调整为 `In Progress`，后续继续把更多页面读取从静态 bootstrap 平滑迁出。
- `FE-003`：已在 `codex/integration-phase1-delivery` 提交 `d5a748f`，会话页已开始消费 workspace stream 事件，把消息、Trace、Context 的实时变化覆盖到当前壳层视图。
- `FE-004`：已在 `codex/integration-phase1-delivery` 提交 `44c6c1f`，右侧 Context 面板开始优先读取真实 `/context` 数据，状态更新为 `Done`。
- `FE-005`：Settings / Capabilities 已从 provider 覆盖层收口到明确的远端读取链路，会话页同时补齐 project / conversation / message / trace 的 workspace API 读取，结合构建验证与联调记录，状态更新为 `Done`。
- `FE-006`：已在 `codex/integration-phase1-delivery` 提交 `1a92569`，Files / Memory 页面从占位页升级为远端优先、失败回退的最小浏览页，状态更新为 `Done`。
- `QA-001`：测试计划与记录模板已形成独立提交 `2c69dd3`，状态更新为 `Done`。
- `QA-002`：`apps/server/src/test/java/com/loom/server/LoomApiIntegrationTest.java` 已覆盖 workspace 主链路、action 查询成功路径、`ACTION_NOT_FOUND` 错误态与 SSE 事件断言，状态更新为 `Done`。
- `QA-003`：前端关键路径已新增“提交消息后消费 SSE 并刷新 bootstrap”的构建验证基线，状态调整为 `In Progress`，后续补手测清单与联调截图。
- `QA-004`：联调检查单与当前联调基线已补齐，状态更新为 `Done`。
- `QA-005`：发布前 `go / no-go` 检查单已补齐，状态更新为 `Done`。
- `PM-001`、`PM-003`、`PM-004`：文档、风险台账、分支恢复与集成分支编排持续维护中，生产服务器尚未进入使用窗口。
- 当前下一批待推进需求：`FE-001 / FE-002 / FE-003` 与 `BE-001 / BE-002 / BE-003` 的进一步产品化收口，以及 `QA-003` 的前端关键路径自动化或更完整手测证据。
## A. Refactor Wave Supplement (Historical, 2026-04-09)

### 0.1 Additional requirements

#### Architecture / PM

| Requirement ID | Priority | Status | Title | Source | Dependency | Acceptance |
| --- | --- | --- | --- | --- | --- | --- |
| PM-006 | P0 | Done | Orchestrate the refactor wave, maintain stage gates, integration status, and acceptance delivery. | 2026-04-09 refactor execution plan | ARC-006 | The requirement pool, implementation status, validation summary, and acceptance entry are kept current through the whole wave. |
| ARC-006 | P0 | Done | Freeze the convergence plan from the temporary `workspace` aggregate to facade-only bootstrap assembly plus domain modules. | 2026-04-09 refactor execution plan | ARC-003, ARC-005 | The backend keeps `workspace` only as a transition facade/bootstrap assembler, and new domain ownership is explicit for conversation, context, memory, trace, and stream. |

#### Backend

| Requirement ID | Priority | Status | Title | Source | Dependency | Acceptance |
| --- | --- | --- | --- | --- | --- | --- |
| BE-008 | P0 | In Progress | Persist `project / conversation / message / action / run / run_step` and replace in-memory business truth. | 2026-04-09 refactor execution plan | ARC-006 | Restart-safe project, conversation, message, and trace data are stored in DB and no longer owned by `WorkspaceStateService` collections. |
| BE-009 | P0 | Done | Implement the `context` module with turn-based assembly and persisted `context_snapshot`. | 2026-04-09 refactor execution plan | BE-008 | Context reads come from assembled/persisted snapshots, and `context snapshot` history is queryable by conversation. |
| BE-010 | P0 | Done | Implement the `memory` module with project/conversation memory CRUD, suggestion lifecycle, and accept/reject flow. | 2026-04-09 refactor execution plan | BE-008 | Memory items and memory suggestions are persisted, scoped, queryable, and writable through stable APIs. |
| BE-011 | P1 | In Progress | Demote `WorkspaceStateService` to a facade and demote `/api/bootstrap` to hydration-only snapshot assembly. | 2026-04-09 refactor execution plan | BE-008, BE-009, BE-010 | Bootstrap no longer acts as long-lived business truth, and `workspace` no longer owns the business state collections. |

#### Frontend

| Requirement ID | Priority | Status | Title | Source | Dependency | Acceptance |
| --- | --- | --- | --- | --- | --- | --- |
| FE-007 | P0 | Done | Converge the frontend business data source to `workspace API + SSE overlay`, with bootstrap only for first-screen hydration. | 2026-04-09 refactor execution plan | BE-008 | After remote data is loaded, conversation/context/trace/settings/capabilities/memory are rendered from real APIs instead of bootstrap business payloads. |
| FE-008 | P1 | Done | Move Memory page and memory suggestions into the unified provider data flow. | 2026-04-09 refactor execution plan | FE-007, BE-010 | Memory page and suggestion UX read from shared provider state and no longer maintain page-local fallback business logic. |
| FE-009 | P1 | In Progress | Remove bootstrap business override logic from `effectivePayload` and keep overlay only for the active streaming conversation. | 2026-04-09 refactor execution plan | FE-007, BE-011 | `payload + remote + overlay` three-way business mixing is removed; overlay is scoped to the active stream only. |

#### QA / Ops

| Requirement ID | Priority | Status | Title | Source | Dependency | Acceptance |
| --- | --- | --- | --- | --- | --- | --- |
| QA-006 | P0 | Done | Add backend integration coverage for restart persistence, context assembly, memory lifecycle, and stream replay. | 2026-04-09 refactor execution plan | BE-008, BE-009, BE-010 | Automated backend tests cover persistence across restart/bootstrap, context snapshot reads, memory CRUD/suggestions, and successful/failed SSE flows. |
| QA-007 | P0 | In Progress | Add frontend validation for single-source rendering, SSE overlay, memory suggestions, and bootstrap downgrade behavior. | 2026-04-09 refactor execution plan | FE-007, FE-008, FE-009 | Frontend validation proves remote-data takeover, stream updates, memory suggestion UX, and non-regression of route/project/session switching. |
| OPS-001 | P1 | Done | Prepare local acceptance deployment steps, smoke commands, and final verification notes. | 2026-04-09 refactor execution plan | QA-006, QA-007 | Local backend and web services can be started with documented commands, smoke-checked, and handed over for acceptance. |

### 0.2 Dependency order

1. `ARC-006`
2. `BE-008`
3. `BE-009` and `BE-010` in parallel
4. `FE-007`
5. `FE-008` and `FE-009`
6. `QA-006` and `QA-007`
7. `OPS-001`

### 0.3 Execution notes

- This wave uses a single integration branch and a single baseline checkpoint commit: `PM-006 chore: checkpoint pre-refactor integration baseline`.
- Runtime directories such as `.codex-runtime/` and `apps/server/.local/` stay outside version control and are excluded from delivery artifacts.
