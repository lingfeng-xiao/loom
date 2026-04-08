# loom 需求池

生效日期：2026-04-08

设计来源：
- `docs/loom-phase1-architecture-design.md`
- `docs/loom-springboot-backend-module-design.md`
- `docs/loom-java-package-structure.md`
- `docs/development-spec.md`
- 当前仓库实现现状（`apps/web`、`apps/server`、`packages/contracts`）

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
| ARC-001 | P0 | Ready | 冻结 Phase 1 领域边界，明确 Project、Conversation、Message、Context Snapshot、Memory、Action、Run、File Asset 的最小定义与状态枚举。 | 架构设计、第 6/8/9/12/17 节 | 无 | 输出统一术语、对象定义、状态口径，供前后端和测试直接引用。 |
| ARC-002 | P0 | Ready | 冻结前后端最小合同：bootstrap 之后新增 project、conversation、message、context、trace、settings 的 REST 结构和 SSE 事件结构。 | 架构设计、第 16/18 节；后端模块设计、第 8/10/11 节 | ARC-001 | 形成 API / event 契约基线，包含字段、错误口径、时间字段和分页约定。 |
| ARC-003 | P0 | Ready | 冻结 Phase 1 模块化单体边界和包结构迁移策略，确认从 `com.template.server` 向 loom 领域包迁移的顺序。 | 后端模块设计、第 3/5/7 节；Java 包结构文档 | ARC-001 | 产出模块边界和迁移顺序，禁止跨模块直连仓储。 |
| ARC-004 | P1 | Ready | 冻结内部执行与 OpenClaw 的阶段边界，明确 Phase 1 中哪些能力保留可见入口、哪些不进入真实执行链路。 | 架构设计、第 4/13/20/21 节 | ARC-002 | 输出一份范围说明，避免前后端把 OpenClaw 误接入主链路。 |
| ARC-005 | P1 | Ready | 制定 Phase 1 数据模型和迁移清单，覆盖 `project` 到 `run_step` 的首批表结构。 | 架构设计、第 17 节；后端模块设计、第 9/12 节 | ARC-001, ARC-003 | 表结构、迁移顺序、主外键关系和审计字段口径明确。 |

### 3.2 前端开发

| 需求 ID | 优先级 | 状态 | 需求项 | 来源 | 依赖 | 验收标准 |
| --- | --- | --- | --- | --- | --- | --- |
| FE-001 | P0 | In Progress | 将现有三栏壳层从纯 fallback/bootstrap 展示升级为“可切换真实数据源”的工作台，保留加载态、错态和空态。 | 当前 `apps/web` 实现现状 | ARC-002 | 本地无后端时可回退，有后端时优先读真实接口，状态表现完整。 |
| FE-002 | P0 | Ready | 接入项目与会话基础能力：项目摘要、会话列表、消息列表、模式切换、草稿输入。 | 架构设计、第 7/8/18 节 | ARC-002, BE-002 | 会话页不再只消费静态 bootstrap，主链路基于真实接口渲染。 |
| FE-003 | P0 | Ready | 接入消息流式输出与 Trace 面板实时更新，完成 message stream、thinking summary、run step 的前端订阅与渲染。 | 架构设计、第 14/18 节；后端模块设计、第 10 节 | ARC-002, BE-003 | 会话回复、思考摘要、Trace 状态变化通过统一 SSE 合同实时更新。 |
| FE-004 | P1 | Ready | 将 Context 右侧面板升级为真实数据面板，展示目标、约束、摘要、引用文件、未闭环事项。 | 架构设计、第 7/10 节 | ARC-002, BE-004 | Context 面板支持读真实上下文快照，字段命名和排序一致。 |
| FE-005 | P1 | Ready | 将 Capabilities 与 Settings 页面改为真实读模型，并支持分区切换、配置摘要、风险提示。 | 架构设计、第 13/15 节 | ARC-002, BE-005 | 页面数据来自服务端，不再完全依赖静态 overview。 |
| FE-006 | P2 | Ready | 落地 Files / Memory 页面首版壳层，支持列表、空态、入口联动，为后续 Phase 1.5 做准备。 | 架构设计、第 9/11 节 | ARC-002, BE-006 | Files / Memory 不再是纯占位页，具备最小浏览能力。 |

### 3.3 后端开发

| 需求 ID | 优先级 | 状态 | 需求项 | 来源 | 依赖 | 验收标准 |
| --- | --- | --- | --- | --- | --- | --- |
| BE-001 | P0 | In Progress | 完成服务端命名与骨架迁移：从 template 命名迁到 loom 领域命名，并建立模块化包结构。 | 当前 `apps/server` 现状；Java 包结构文档 | ARC-003 | 根包、配置、启动类、合同命名对齐 loom，迁移后测试仍可运行。 |
| BE-002 | P0 | Ready | 实现 Project / Conversation / Message 最小可用读写接口，形成“提交消息前”的真实主数据基础。 | 架构设计、第 6/16/18/19 节 | ARC-001, ARC-002, ARC-005 | 至少支持项目详情、会话列表、消息列表、创建会话、提交用户消息。 |
| BE-003 | P0 | Ready | 实现 stream 模块和 SSE 事件合同，支持 `message.delta`、`message.done`、`thinking.summary.*`、`trace.step.*` 等事件。 | 后端模块设计、第 9/10 节 | ARC-002, BE-002 | 前端可稳定订阅单一流入口，断开重连和完成态口径一致。 |
| BE-004 | P1 | Ready | 实现 context 模块首版：上下文组装、摘要刷新、决策与 open loops 快照查询。 | 架构设计、第 10/18 节；后端模块设计、第 4.3 节 | ARC-001, ARC-002, ARC-005 | 提供 Context 面板读接口和刷新接口，结构与文档一致。 |
| BE-005 | P1 | Ready | 实现 settings / capability 最小读接口，包含 model profiles、skills、MCP、routing、memory policy 的读取视图。 | 架构设计、第 13/15 节；后端模块设计、第 4.6/4.8 节 | ARC-002, ARC-005 | Capabilities / Settings 页面可读取真实摘要数据。 |
| BE-006 | P1 | Ready | 实现 action / trace 最小链路，支持创建 action、run、run_step 并对外查询。 | 架构设计、第 12/14 节；后端模块设计、第 4.5 节 | ARC-001, ARC-002, BE-003 | Trace 面板有真实 run step 数据来源，失败和等待态可区分。 |
| BE-007 | P2 | Ready | 实现 file / memory 首版数据模块，支持项目文件元数据与分层 memory 列表。 | 架构设计、第 9/11 节 | ARC-001, ARC-002, ARC-005 | Files / Memory 页面具备真实列表读取能力。 |

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
| QA-001 | P0 | In Progress | 基于新架构补 Phase 1 测试计划，覆盖会话主链路、Trace、Context、Settings、降级策略。 | 开发规范、第 9 节；当前测试文档缺位 | ARC-002, PM-001 | 输出测试范围、环境、入口数据、回归清单和发布建议口径。 |
| QA-002 | P0 | Ready | 扩展后端集成测试，覆盖 project / conversation / message / stream / settings 新接口。 | 当前 `apps/server` 测试现状 | BE-002, BE-003, BE-005 | 新接口具备 MockMvc 或集成测试，关键错误态可回归。 |
| QA-003 | P0 | Ready | 建立前端冒烟与关键路径验证，覆盖欢迎页进入、会话切换、模式切换、流式回复、右侧面板切换。 | 当前 `apps/web` 实现现状 | FE-002, FE-003, FE-004 | 主链路有自动化或明确手测清单，可在每次合并前执行。 |
| QA-004 | P1 | Ready | 建立前后端联调检查单，明确契约字段、事件顺序、断线重连、空态和错态。 | 开发规范、第 4.5 节 | ARC-002, FE-003, BE-003 | 联调结果可回填到需求条目，不再只留口头结论。 |
| QA-005 | P1 | Ready | 建立发布前验收与回归建议，区分 P0 主链路、P1 配置面、P2 占位能力。 | 开发规范、第 4.6/4.7 节 | PM-005 | 发布前有一份明确的 go / no-go 检查清单。 |

## 4. 当前执行顺序建议

1. 先完成 `ARC-001`、`ARC-002`、`ARC-003`，冻结领域边界、合同和迁移路线。
2. 并行推进 `BE-001`、`BE-002`、`FE-001`，把前端壳层和后端真实主数据接起来。
3. 再推进 `BE-003`、`BE-004`、`BE-005` 与 `FE-003`、`FE-004`、`FE-005`，补齐流式、Context、Trace、Settings。
4. `BE-006`、`FE-006`、`QA-004`、`QA-005` 作为 Phase 1 后半段收口项推进。

## 5. 维护说明

- 本文档从 2026-04-08 起重新启用，旧需求池内容不再恢复。
- 后续新增需求必须以本需求池为唯一入口，并同步回填状态、阻塞项、文档影响和测试结果。
- 如果设计边界发生变化，先更新架构文档和本需求池，再进入实现。
