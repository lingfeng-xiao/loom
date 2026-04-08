# loom Phase 1 风险与发布记录

生效日期：2026-04-08

## 1. 目的

本文件用于记录 Phase 1 并发开发期间的核心风险、生产机使用边界、发布门禁和回退要求。

## 2. 当前高风险项

| 风险 ID | 风险描述 | 影响 | 当前策略 | Owner |
| --- | --- | --- | --- | --- |
| R-001 | 当前工作树含大量未提交改动，直接从 `main` 并发开发会互相污染 | 高 | 已切出 `codex/pm-phase1-baseline` 作为起点，禁止后续直接从脏 `main` 起分支 | PM |
| R-002 | Server 端仍保留 template 命名，loom Phase 1 模块迁移时容易出现命名与部署耦合 | 高 | 先冻结迁移顺序，再推进代码改造；任何服务名/镜像名变化必须同步运行文档 | 架构师 / 后端 |
| R-003 | 前端当前主要依赖 bootstrap 和 fallback 数据，接线真实接口时容易漂移 | 高 | 先冻结 contracts，再允许前端接线；`packages/contracts` 在冻结后默认只读 | 架构师 / 前端 |
| R-004 | SSE 合同未冻结前，前后端容易各自发明事件格式 | 高 | `ARC-002` 完成前，前端只可预留适配层，不可私定事件协议 | 架构师 / 前后端 |
| R-005 | 开发期可直连生产机调试，若无门禁容易造成线上污染 | 高 | 仅 PM 可执行 `ssh jd`；先只读后写；写操作必须可回退且有记录 | PM |
| R-006 | 并发子智能体共享同一工作区时发生分支串扰，导致 lane 提交和未提交草稿混在一起 | 高 | 已关闭全部子智能体，保留 `codex/recovery-phase1-mixed-state` 恢复快照，改为单线程继续清理 | PM |

## 3. 生产机使用策略

生产服务器：`ssh jd`

硬规则：

- 只有 PM 可以登录生产机
- 并发工作智能体不得直接对生产机执行命令
- 本地验证未通过前，不得上生产机做写操作
- 生产写操作前，必须确认 `/opt/template/scripts/remote-rollback.sh` 可用
- 生产写操作前，必须确认最近一次成功快照存在于 `/opt/template/state/last_successful.env`

允许的只读操作：

- `docker ps`
- 容器日志查看
- 健康检查
- 配置与路径核对

受控写操作：

- 使用现有 deploy / release / rollback 脚本进行候选部署
- 仅在联调或候选验收窗口内执行
- 必须记录目的、执行人、开始时间、结果、回退结论

## 4. 发布门禁

进入发布候选前必须满足：

- 需求池对应条目已回填
- 相关设计/合同/测试文档已更新
- 本地测试和构建通过
- 联调结果已记录
- 风险与回退说明已确认

## 5. 回退规则

若生产验证失败：

1. 立即停止继续写操作
2. 记录失败接口、日志和时间点
3. 执行 `/opt/template/scripts/remote-rollback.sh`
4. 重新核对 `last_successful.env`
5. 将问题回填到需求池、测试记录和 PR 风险项

## 6. 维护要求

- 每次进入生产机窗口前更新本文件
- 每次新增高风险变更时补充风险项
- 发布完成后补最终结论：成功 / 回退 / 待重试

## 7. 当前恢复结论

- 已保留 PM 基线分支：`codex/pm-phase1-baseline`
- 已保留后端 lane 分支：`codex/backend-be-core-phase1`
- 已保留 QA lane 分支：`codex/qa-phase1-validation`
- 已新增恢复快照分支：`codex/recovery-phase1-mixed-state`
- 在完成提交审计和择取前，不再开启新的并发智能体

## 8. 最新 PM 记录（2026-04-08）

- 当前执行分支：`codex/integration-phase1-delivery`
- 已新增架构提交：`de52aa5` `ARC-002 docs: freeze phase1 contracts and module boundaries`
- 已新增架构提交：`82877a7` `ARC-002 docs: freeze phase1 contract baseline`
- 新增后端提交：`687d57d` `BE-002 feat: add phase1 workspace api`
- 新增后端提交：`0fdb1e9` `BE-003 feat: expand workspace stream events`
- 新增后端提交：`13143aa` `BE-004 feat: add context refresh and capability overview`
- 已新增测试提交：`9947eb7` `QA-004 docs: add debug and smoke records`
- 当前前端状态：已完成消息提交到真实后端、SSE 事件消费、Context 真数据读取，以及 Settings / Capabilities 远端读模型覆盖；仍需补联调证据
- 本地验证：
  - `apps/server` `./mvnw -q test` 通过
  - `apps/web` `npm run build` 通过
- 生产机状态：未使用 `ssh jd`，无只读检查、无写操作、无回退动作
- 发布门禁结论：仍未达到生产验证窗口，原因是 `BE-006 / FE-006` 尚未完成，且 `FE-005 / QA-004 / QA-005` 仍缺最终联调与放行证据

## 9. 当前并发风险控制（2026-04-08）

- 已重新开启角色并发，但执行方式从“共享脏工作区并发”调整为“按目录隔离的独立写入责任”
- 当前并发门禁：
  - ARC 不得修改 `apps/web`、`apps/server`
  - BE 不得修改 `apps/web`、`docs`
  - FE 不得修改 `apps/server`、`docs`
  - QA 不得修改业务代码
- 当前新增风险：
  - R-007：ARC 文档与 BE/FE 现有实现可能存在轻微偏差，需由 PM 在集成时校准
  - R-008：FE 与 BE 在 Context / Settings 的字段命名上可能产生时间差，需以 ARC lane 的冻结结果为最终口径
  - R-009：Capabilities / Settings 当前通过 provider 覆盖读模型接入，仍需页面级联调确认展示口径和降级策略
- 当前缓解措施：
  - PM 在合入前统一复核 contracts / docs / apps 的一致性
  - 所有 lane 完成后必须再次执行 `apps/server` 测试和 `apps/web` 构建
  - 若 `FE-005 / QA-004 / QA-005` 仍无联调证据，PM 不开启生产机只读检查窗口
