# loom Phase 1 Kickoff Board

生效日期：2026-04-08

上游依据：

- `docs/requirements-pool-spec.md`
- `docs/loom-v1-role-breakdown.md`
- `docs/loom-phase1-architecture-design.md`
- `docs/loom-springboot-backend-module-design.md`
- `docs/development-spec.md`

当前编排策略：

- 采用“先冻结再并发”
- 当前工作基线分支：`codex/pm-phase1-baseline`
- 当前恢复快照分支：`codex/recovery-phase1-mixed-state`
- PM 负责总调度、文档维护、风险控制和生产机窗口
- 并发工作智能体共 4 个：架构师、前端开发、后端开发、测试

## 1. Baseline Freeze

基线目标：

- 冻结当前 shell / docs / contracts 的起始状态
- 禁止后续智能体直接从脏 `main` 工作树起分支
- 先明确需求池、任务板、测试计划和风险/发布边界

基线检查项：

| 项目 | 当前状态 | 说明 |
| --- | --- | --- |
| 分支隔离 | 已完成 | 当前工作迁移到 `codex/pm-phase1-baseline` |
| 需求池 | 已完成 | 需求已按 `ARC / FE / BE / PM / QA` 建模 |
| Kickoff 板 | 进行中 | 本文档为当前执行面板 |
| 测试计划 | 进行中 | 需与合同冻结结果联动更新 |
| 风险/发布记录 | 进行中 | 需纳入生产机使用边界 |

## 2. Agent Board

| Lane | 角色 | 智能体名称 | 主要需求 | 建议分支 | 状态 | 启动条件 | 写入范围 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| PM | PM / Orchestrator | 当前主线程 | `PM-001` ~ `PM-005` | `codex/pm-phase1-baseline` | In Progress | 已启动 | `docs/requirements-pool-spec.md`、本文档、测试/发布文档 |
| ARC | webAI 架构师 | `architect-agent` | `ARC-001` ~ `ARC-005` | `codex/architect-arc-contract-freeze` | Done | 基线冻结完成 | 架构文档、契约文档、`packages/contracts` |
| BE | 后端开发 | `backend-agent` | `BE-001` ~ `BE-006` | `codex/backend-be-core-phase1` | In Progress | `ARC-002`、`ARC-003` 冻结 | `apps/server` 与后端测试 |
| FE | 前端开发 | `frontend-agent` | `FE-001` ~ `FE-005` | `codex/frontend-fe-shell-integration` | Paused | `ARC-002` 冻结 | `apps/web` |
| QA | 测试 | `qa-agent` | `QA-001` ~ `QA-005` | `codex/qa-phase1-validation` | Done | 基线冻结完成 | 测试计划、测试记录、必要测试文件 |

状态口径：

- `Ready`：可立即启动
- `Blocked`：等待上游冻结或集成基线
- `In Progress`：已开始产出
- `Paused`：已有尝试产出，但因工作区串扰暂停继续写入

## 2.1 Current Branch Map

| 分支 | 当前定位 | 当前提交 | 备注 |
| --- | --- | --- | --- |
| `codex/pm-phase1-baseline` | PM 基线 | `7228556` | 已落地需求池、kickoff、测试/风险/发布基线 |
| `codex/backend-be-core-phase1` | 后端 lane | `282a2f8` | 已落地 `BE-001` 命名迁移与测试通过 |
| `codex/qa-phase1-validation` | QA lane | `2c69dd3` | 已落地 `QA-001` 测试矩阵与记录模板 |
| `codex/frontend-fe-shell-integration` | 前端 lane | `d7cb1e4` | 未形成独立提交，当前需从恢复快照中择取 |
| `codex/architect-arc-contract-freeze` | 架构 lane | `d7cb1e4` | 未形成独立提交，当前需从恢复快照中择取 |
| `codex/recovery-phase1-mixed-state` | 恢复快照 | `facff60` | 保存了并发执行期间的混合工作树，禁止直接合并 |
| `codex/integration-phase1-delivery` | 单线程集成交付分支 | `82877a7` | 当前主执行分支，已连续落地 ARC、BE、FE、QA 本轮基线产物，正继续推进 Context / Settings |

## 3. Iteration Order

### Wave 0

- PM 完成基线冻结与文档恢复
- 补齐 kickoff board、测试计划、风险/发布记录
- 锁定 git、PR、生产机使用规则
- 已补充恢复快照分支，保全并发执行期间的混合工作区

### Wave 1

- 架构师完成 `ARC-001`、`ARC-002`、`ARC-003`
- 测试同步起草 `QA-001`，但不对未冻结接口做硬断言
- 实际结果：`QA-001` 已形成独立提交；`ARC` lane 暂停，待单线程审计恢复

### Wave 2

- 后端启动 `BE-001`、`BE-002`、`BE-003`
- 前端启动 `FE-001`、`FE-002`
- 测试启动 `QA-003`、`QA-004`
- 实际结果：`BE-001` 已形成独立提交并通过本地测试；`BE-002` 已在 `codex/integration-phase1-delivery` 形成提交 `687d57d`；`FE-001 / FE-002` 已恢复到单线程集成交付分支并通过前端构建

### Wave 3

- 后端推进 `BE-004`、`BE-005`、`BE-006`
- 前端推进 `FE-003`、`FE-004`、`FE-005`
- 测试补齐联调和发布前回归结论
- 2026-04-08 新一轮并发已重新开启，但写入范围改为严格隔离：
  - ARC lane：仅 `docs`、`packages/contracts`
  - BE lane：仅 `apps/server`
  - FE lane：仅 `apps/web`
  - QA lane：仅测试文档与验证记录
- 本轮 PM 主线程不做业务实现，只做文档门禁、风险同步、结果集成和最终验证

### Wave 4

- PM 组织联调、生产验证、验收和合并
- `FE-006`、`BE-007` 作为后续 Phase 1.5 入口保留在 Backlog

## 4. Merge Gates

每条分支进入可合并状态前必须满足：

- 关联需求 ID
- 说明影响范围
- 列出文档更新清单
- 附测试结果或豁免说明
- 附风险与回退说明

固定合并顺序：

1. 基线
2. 架构 / 合同
3. 后端主数据
4. 前端接线
5. SSE / Trace
6. Context / Settings
7. QA 结论

## 5. Production Window Rule

- `ssh jd` 仅允许 PM 操作
- 开发期上生产机优先做只读检查：容器状态、日志、健康检查、配置路径
- 任何写操作必须在本地验证通过后进入受控窗口执行
- 写操作前必须确认 `/opt/template/scripts/remote-rollback.sh` 可用，且最近一次成功快照存在

## 6. Daily Maintenance

PM 每轮至少回填以下内容：

- 当前活跃分支
- 各 lane 状态变化
- 新阻塞项和风险项
- 文档是否已同步
- 是否满足进入联调或生产验证的条件

## 7. Recovery Note

当前恢复策略：

- 所有并发子智能体已关闭
- 单线程继续推进
- 先以 `codex/pm-phase1-baseline` 作为 PM 主控制分支
- 以 `codex/backend-be-core-phase1` 和 `codex/qa-phase1-validation` 保留已验证提交
- 以 `codex/recovery-phase1-mixed-state` 保存前端、contracts、文档的混合草稿，后续按需择取

## 8. 最新执行快照（2026-04-08）

- 当前活跃分支：`codex/integration-phase1-delivery`
- 已完成的可追踪提交：
  - `de52aa5` `ARC-002 docs: freeze phase1 contracts and module boundaries`
  - `82877a7` `ARC-002 docs: freeze phase1 contract baseline`
  - `9947eb7` `QA-004 docs: add debug and smoke records`
  - `282a2f8` `BE-001 refactor: migrate server package root to loom`
  - `687d57d` `BE-002 feat: add phase1 workspace api`
  - `0fdb1e9` `BE-003 feat: expand workspace stream events`
  - `2c69dd3` `QA-001 docs: add phase1 test matrix`
  - `80a5084` `FE-002 feat: wire composer submission to workspace api`
  - `d5a748f` `FE-003 feat: apply workspace stream events in conversation view`
- 当前未进入生产机窗口；`ssh jd` 仍未执行
- 本地验证结果：
  - `apps/server`：`./mvnw -q test` 通过
  - `apps/web`：`npm run build` 通过
- 当前下一顺位目标：继续推进 `BE-004 / FE-004 / BE-005 / FE-005` 的 Context 与 Settings 真数据读写

## 9. 当前并发轮次（2026-04-08）

| Lane | 当前目标 | 写入范围 | 当前状态 |
| --- | --- | --- | --- |
| ARC | `ARC-001` ~ `ARC-005` 合同冻结、模块边界、迁移顺序、OpenClaw 范围说明 | `docs`、`packages/contracts` | Done |
| BE | `BE-004`、`BE-005`，并为 `BE-006` 打基础 | `apps/server` | In Progress |
| FE | `FE-004`、`FE-005`，在现有 `FE-002/003` 上接 Context / Capabilities / Settings 真数据 | `apps/web` | In Progress |
| QA | `QA-004`、`QA-005`，并补 `QA-003` 可执行验证记录 | `docs` | Done |
| PM | 维护需求池、kickoff、风险台账、测试记录和集成门禁 | `docs` | In Progress |

本轮集成顺序：

1. ARC 合同冻结结果
2. BE Context / Settings 读接口
3. FE Context / Settings 接线
4. QA 联调与 go / no-go 文档
5. PM 统一验证、回填和决定是否进入生产机只读检查窗口
