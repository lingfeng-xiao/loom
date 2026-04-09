# Loom 会话闭环收敛计划

生效日期：2026-04-09

设计来源：

- 当前仓库实现现状：`apps/web`、`apps/server`、`packages/contracts`
- `docs/requirements-pool-spec.md`
- `docs/deployment.md`
- `docs/release-runbook.md`
- `docs/rollback.md`

## 1. Goal

本轮唯一主线是把 loom 收敛成一个干净、稳定、低耦合、可恢复、可发布的项目内会话系统。

本轮目标不是扩展产品面，而是完成“产品级会话闭环”：

- 项目内创建和切换会话
- 消息发送与流式回复
- 上下文组装、展示与刷新
- 记忆建议、接受、拒绝与回流
- 页面刷新与服务重启后的恢复
- 单机生产可运维的最小发布与回滚闭环

## 2. Product Boundary

### 2.1 保留范围

当前产品只保留以下能力：

- Project：最小项目容器
- Conversation：会话列表、会话详情、会话恢复
- Message：消息发送、流式回复、失败恢复
- Context：当前会话上下文组装与展示
- Memory：项目/会话记忆与记忆建议闭环
- Stream：最小 SSE 流式协议与重放能力
- Ops baseline：最小部署、smoke、rollback 闭环

### 2.2 删除范围

以下内容不属于当前产品级会话范畴，默认删除、下线或归档：

- `Chat / Plan / Action / Review` 模式概念
- “本地快照 / 远端数据 / 自动降级 / 当前数据源”等实现概念
- `Capabilities`、`Settings`、`Files`、`OpenClaw` 等非当前闭环必要产品面
- 与上述概念绑定的页面、路由、接口字段、文案、测试、seed/demo 数据、脚本和文档

### 2.3 产品表达规则

- 用户只看见“项目”“会话”“上下文”“记忆”
- 内部 hydration、恢复、重放、降级都不是产品概念
- 系统不能把技术实现细节暴露给用户理解

## 3. Target Architecture

### 3.1 后端目标结构

后端只保留以下模块：

- `common`
- `project`
- `conversation`
- `context`
- `memory`
- `stream`

结构要求：

- `conversation` 是主流程中心
- `context` 负责 `ContextPackage`、`ContextSnapshot` 和 context panel read model
- `memory` 负责 `MemoryItem`、`MemorySuggestion` 和 retrieval
- `stream` 负责 conversation SSE、事件发布、重放
- `project` 只保留最小容器职责
- `workspace` 不再持有长期业务真相；保留则只能做极薄 facade

依赖规则：

- `conversation` 只能通过接口依赖 `context`、`memory`、`stream`
- `context` 只能依赖 conversation query 接口与 memory retrieval 接口
- `memory` 不反向依赖 conversation 业务逻辑
- `stream` 不拥有业务真相
- 禁止跨模块直接访问别人的 repository

### 3.2 前端目标结构

前端只保留以下业务状态域：

- `project store`
- `conversation store`
- `context store`
- `memory store`
- `session-ui store`

结构要求：

- 不再由单一大 Provider 长期混合多个业务真相源
- 首屏 hydration 仅作为内部实现
- hydrate 完成后，业务真相只来自真实 API
- active conversation 的流式 overlay 仅作为短暂渲染层，不作为产品概念

## 4. Clean-up Scope

本轮需要同步清理以下范围：

### 4.1 代码

- 删除已废弃概念、页面、模块、类型、接口字段
- 移除过渡态 fallback 业务逻辑
- 拆解中心化巨型 service 和总 DTO 容器

### 4.2 页面与路由

- 删除非核心页面入口
- 删除无效路由、query 参数、导航项、占位页
- 删除模式与数据源相关 UI

### 4.3 契约与类型

- 删除 `mode`、`requestedMode`、数据源状态字段
- 共享 contracts 成为唯一业务契约来源
- 前后端类型、DTO、测试、文档同步收口

### 4.4 配置与部署

- 收口 `.env.example`、compose、Spring 配置、脚本变量
- 清理 `template` / `loom` 混杂命名
- 移除被删除模块相关的运行配置和部署残留

### 4.5 文档

- 新增本文件作为当前唯一上游总计划
- 更新需求池、索引、README
- 清理或归档已过期文档
- 现行文档只保留当前有效口径

### 4.6 测试与示例数据

- 删除为已移除概念续命的测试与 fixture
- 清理旧 seed/demo 数据
- 测试只服务当前会话闭环与部署闭环

## 5. Standards

本轮建立以下规范，并要求映射到系统本身，而不是停留在纯文本说明。

### 5.1 前端规范

- 目录职责固定：`app / domains / pages / components / sdk / services`
- store 按领域拆分，禁止再引入新的中心化业务状态聚合器
- 页面只能表达当前产品能力，不保留未来占位概念
- 共享类型优先来自 `packages/contracts`
- 路由只承载当前产品概念

### 5.2 后端规范

- 模块固定为 `common / project / conversation / context / memory / stream`
- 每个模块固定分层：`controller / application / domain / infrastructure / dto`
- 禁止跨模块直连 repository
- controller 保持薄，编排进 application，规则进 domain

### 5.3 Git 规范

- 分支命名必须表达范围
- 提交信息必须表达意图和影响面
- 删除类改动必须在 PR 说明中显式列出
- 不允许把无关重构、功能修改、风格调整混成同一批交付

### 5.4 Maven 规范

- Java、Spring Boot、插件版本集中管理
- Maven wrapper 作为统一构建入口
- profile 只保留当前真实使用的环境
- artifact 和构建产物命名统一为 `loom`

### 5.5 发布规范

- 固定链路：`preflight -> candidate deploy -> smoke -> promote -> rollback-ready`
- 不允许绕过脚本直接改线上运行态
- rollback baseline 必须先于发布确认

### 5.6 文件规范

- 目录结构表达当前系统边界
- 文件命名语义化
- 过期文件删除或归档，不允许散落主路径

### 5.7 契约规范

- `packages/contracts` 作为共享业务契约唯一来源
- 字段删除必须同步更新前端、后端、测试、文档
- SSE 事件名和 payload 统一管理

### 5.8 测试规范

- 只围绕当前会话闭环、恢复、流式、部署闭环建立测试
- 删除概念时，测试和 fixture 必须同步删除
- acceptance 和 smoke 清单必须入仓库

### 5.9 文档规范

- 当前有效文档只保留一份口径
- 过期文档要么删除，要么进入归档区
- 文档索引必须明确区分现行与历史

### 5.10 配置规范

- 环境变量有唯一来源与唯一说明
- `.env.example` 必须与真实运行变量对齐
- 删除模块后，配置项同步删除

### 5.11 日志与观测规范

- 保留 request id、conversation id、stream id
- 关键错误、发布、rollback 行为必须可追踪
- 不保留噪音型日志

## 6. Role Breakdown

### 6.1 ARC

- 冻结产品边界，只保留项目内会话闭环
- 冻结目标架构、模块依赖与保留模块
- 冻结规范清单及其系统映射规则
- 冻结文档清理、归档与现行口径

### 6.2 FE

- 删除模式概念、数据源概念、非核心页面与入口
- 把前端状态层重组为多 domain store
- 会话页、上下文、记忆页收敛为最小产品面
- 清理旧类型、旧服务、旧路由、旧 seed/demo 数据

### 6.3 BE

- 把后端模块收敛为 `project / conversation / context / memory / stream / common`
- 持久化会话、消息、run 数据
- 删除内存业务真相源、mode 语义和数据源语义
- 完成会话、上下文、记忆、流式恢复闭环

### 6.4 PM

- 把本计划纳入唯一上游并统一需求池引用
- 维护现行文档清单、归档清单、删除清单
- 维护波次节奏、依赖、验收出口和 go/no-go 口径

### 6.5 QA

- 建立会话闭环验收矩阵
- 建立清洁度回归检查清单
- 覆盖流式、恢复、context、memory 和删除概念后的回归验证

### 6.6 OPS

- 收敛部署、脚本、env、命名与镜像口径
- 固化 preflight、smoke、promote、rollback 的最小闭环
- 清理已删除模块相关运行配置与部署残留

## 7. Acceptance

### 7.1 文档闭环

- `docs/loom-conversation-closure-plan.md` 成为当前唯一上游总计划
- `docs/index.md` 与 `docs/README.md` 同步引用该文件
- 需求池新增波次全部引用该文件
- 现行文档与归档文档边界明确

### 7.2 需求池闭环

- 新波次需求已按 `ARC / FE / BE / PM / QA / OPS` 拆分完成
- 每条需求都具备 Source、Dependency、Acceptance
- 角色边界明确，不出现无人认领项

### 7.3 会话闭环

- 用户可稳定完成：项目 -> 会话 -> 消息 -> 流式回复 -> context -> memory suggestion -> 刷新继续 -> 重启继续
- 页面上不再出现已删除概念
- 系统中不存在多份长期业务真相源

### 7.4 清洁度闭环

- 代码、页面、路由、文档、脚本、配置、seed/demo、测试中，不再保留非当前闭环所需残留
- 已删除概念在主路径上无残留引用
- 规范项均映射到系统约束，而不是停留在纯文本说明

## 8. Archive Policy

### 8.1 现行文档

以下文档保留为当前有效文档：

- 本计划文档
- 需求池
- 部署、发布、回滚文档
- 测试计划、测试记录、go/no-go、风险记录

### 8.2 归档文档

以下类型文档应进入归档区或明确标注历史参考：

- 与已删除产品概念强绑定的设计文档
- 与已删除页面和模块强绑定的说明文档
- 已被本计划替代的历史波次规划文档

### 8.3 删除规则

以下内容应直接删除，不继续保留：

- 无主临时文档
- 与当前产品边界冲突的占位文档
- 无人维护且不再服务现行系统的脚本说明、页面说明、旧 demo 说明

### 8.4 维护规则

- `active` 口径只认当前索引中的有效文档
- 过期文档不能继续混入 current valid docs
- 任何新需求和变更都必须回填需求池，并引用本计划
