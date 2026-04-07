## Loom

副标题：Project-first Personal AI OS

### Why this name

**Loom** 的核心含义是“织机”。

这个名字很适合这个项目，因为你的系统本质上不是单纯的聊天工具，而是要把这些东西“织”成一个整体：

- Projects：项目上下文
- Memory：长期记忆
- Plans：任务规划
- Skills / MCP / Tools：能力与工具
- Nodes：本地电脑与服务器
- Assets：知识卡片、运维记录、学习沉淀

它传达的是一种很贴切的产品意象：

> 把分散的会话、记忆、工具、节点和资产，织成你自己的长期 AI 工作台。

另外，这个名字也有几个优点：

- 短，好记
- 气质偏冷静、偏工具化，不浮夸
- 适合长期演进成平台
- 既可以作为产品名，也可以作为仓库名、目录名、服务名

建议命名方式：

- 产品名：**Loom**
- 仓库名：**loom**
- 文档代号：**Project Loom**
- 后端服务名：**loom-server**
- 前端应用名：**loom-web**
- 节点代理名：**loom-node**

---

## Formal Plan 正式方案

### 1. Product Positioning 产品定位

Loom 是一个：

**面向个人长期使用的 Project-first Personal AI OS。**

它不是大众聊天产品，不是纯 SaaS，不是单一模型壳子，也不是一开始就做成重型多智能体平台。

它的核心目标是：

- 沉淀个人知识资产
- 管理本地电脑与远程服务器
- 统一 AI 使用入口
- 兼容并吸收主流 AI 能力标准，例如 skills、MCP、外部工具协议
- 形成可长期演进的个人 AI 工作台

---

### 2. Benchmark Inspirations 参考母型

本方案采用“深度借鉴成熟系统模式，再结合自身目标裁剪”的方法，而不是从零想象一个 AI 产品。

#### 2.1 ChatGPT
重点借鉴：

- Projects：项目作为最高级工作容器
- Memory：长期记忆的显式管理
- 项目内共享上下文与会话组织方式

不借鉴：

- 平台黑箱化
- 资产不完全归用户控制
- 对本地节点与服务器控制能力弱

#### 2.2 Codex
重点借鉴：

- Plan-first：复杂任务先计划，再执行
- Slash Commands：通过 `/` 快速触发高频命令
- 复杂任务与工具调用联动

不借鉴：

- 过度偏向代码场景
- 一开始就重度 IDE 绑定

#### 2.3 Claude Code
重点借鉴：

- Skills：把能力封装成正式能力包
- Project Memory：项目级持久指令与记忆
- 命令体系与工程化上下文组织

不借鉴：

- 过强的终端优先交互
- 把大量复杂功能都压在 CLI 中

#### 2.4 OpenClaw
重点借鉴：

- local-first / file-first 的资产观
- agent runtime 与工具扩展思路
- 多工具、多渠道、节点化运行时

不借鉴：

- 早期就做过重的自治 agent 系统
- 一开始就构建复杂插件生态

---

### 3. Product Principles 产品原则

#### 3.1 Project-first
所有核心工作对象默认归属于项目；无项目上下文的内容视为临时内容。

#### 3.2 Memory is structured
记忆必须分层，不允许把历史聊天直接等同于长期记忆。

#### 3.3 Plan before action
复杂任务默认先进入 Plan，再执行，尤其是节点操作、多工具协作、知识专题生成。

#### 3.4 Commands are primitives
Slash commands 不是附属功能，而是系统级操作原语。

#### 3.5 Assets are first-class
知识卡片、运维记录、学习产出、总结文档，都是一等公民对象。

#### 3.6 External capabilities are enhancers
外部 skills、MCP tools、知识源、渠道接入是增强层，不是系统本体。

#### 3.7 Core assets stay owned
你的知识、配置、项目记录、运维记录、节点体系与长期资产必须掌握在自己手里。

---

### 4. Core Product Definition 核心产品定义

Loom 的正式定义如下：

> 一个以项目为容器、以长期记忆为上下文底座、以 Plan 为复杂任务执行模式、以 Slash Commands 为高频操作入口、以 Skills/MCP/Tools 为能力扩展层、以 Obsidian 为主资产仓库、以本地电脑和服务器为可管理节点的个人 AI OS。

---

### 5. Core Object Model 核心对象模型

#### 5.1 Project
系统中的最高级容器。

建议字段：

- id
- name
- type
- description
- default_skills
- default_commands
- bound_nodes
- knowledge_roots
- project_memory_refs
- created_at
- updated_at

建议项目类型：

- Knowledge Project
- Ops Project
- Learning Project

#### 5.2 Conversation
归属于某个 Project 的会话线程。

建议字段：

- id
- project_id
- title
- mode（chat / plan）
- status
- summary
- created_at
- updated_at

#### 5.3 Memory
分层长期记忆对象。

建议分为：

- Global Memory
- Project Memory
- Derived Memory

建议字段：

- id
- scope
- project_id（可空）
- content
- source_type
- source_ref
- status
- priority
- created_at
- updated_at

#### 5.4 Plan
复杂任务的正式对象，而不是普通消息文本。

建议字段：

- id
- project_id
- conversation_id
- goal
- constraints
- steps
- status
- related_skills
- related_tools
- related_nodes
- approval_required
- execution_result
- created_at
- updated_at

#### 5.5 Command
Slash 命令定义对象。

建议字段：

- id
- name
- category
- description
- input_schema
- handler_type
- handler_ref
- enabled
- created_at
- updated_at

#### 5.6 Skill
正式能力包对象。

建议字段：

- id
- name
- version
- description
- trigger_mode
- instruction_ref
- resource_ref
- tool_bindings
- scope
- enabled
- created_at
- updated_at

#### 5.7 Tool
统一工具抽象，来源可能是：

- internal
- node
- mcp
- external

建议字段：

- id
- name
- type
- source
- description
- input_schema
- output_schema
- risk_level
- enabled
- created_at
- updated_at

#### 5.8 Node
本地电脑或服务器节点。

建议字段：

- id
- name
- type
- host
- tags
- status
- last_heartbeat
- metrics_snapshot
- capabilities
- created_at
- updated_at

#### 5.9 Asset
系统沉淀出的正式资产。

建议类型：

- knowledge_card
- ops_note
- learning_card
- summary_note
- structured_markdown

建议字段：

- id
- project_id
- type
- title
- content_ref
- source_conversation_id
- source_plan_id
- source_node_id
- tags
- storage_path
- created_at
- updated_at

---

### 6. System Architecture 系统分层架构

建议采用 7 层架构。

#### 6.1 Product Layer
用户看到的核心产品对象：

- Projects
- Conversations
- Plans
- Commands
- Skills
- Tools
- Nodes
- Memory
- Assets

#### 6.2 UI Layer
Web 前端工作台。

主要界面：

- Project Sidebar
- Chat View
- Plan View
- Project Dashboard
- Asset View
- Node View
- Skill View
- Memory View
- Settings

#### 6.3 Application Layer
Spring Boot 应用编排层，负责：

- 会话编排
- 项目上下文装配
- 记忆装配
- Plan 编排
- Slash command 分发
- 资产沉淀编排
- 节点调度编排

#### 6.4 Capability Layer
正式能力层，放：

- Skills
- Workflows
- Prompt Packs
- Role Profiles

#### 6.5 Tool Gateway Layer
统一工具总线，负责：

- Internal Tools
- Node Tools
- MCP Tools
- Future External Tools

#### 6.6 Knowledge & Memory Layer
负责：

- Global Memory
- Project Memory
- Derived Memory
- Knowledge Card Generation
- Obsidian Connector
- Asset Indexing

#### 6.7 Storage Layer
负责：

- 应用数据库
- 配置文件
- Obsidian 仓库
- 审计日志
- 节点状态快照
- 同步元数据

---

### 7. Deployment Topology 部署拓扑

#### 7.1 总体原则
服务器作为中心节点，本地电脑作为主要使用入口和执行节点之一。

#### 7.2 角色分配
- Server：中心服务节点
- Local PC：用户主入口 + 本地执行节点
- Obsidian Repo：主资产仓库
- Node Agents：本地电脑与服务器上的轻量节点代理

#### 7.3 数据组织
- 应用数据：统一存储在中心数据库
- 资产内容：写入 Obsidian 仓库
- 节点状态：由 agent 主动上报
- 控制命令：由中心服务下发给节点 agent
- 后续飞书：作为轻量入口与通知渠道

---

### 8. Main Storage Strategy 主存储策略

#### 8.1 主资产仓库
第一阶段主资产层选择 **Obsidian / Markdown**，不以 Notion 为主存。

理由：

- Markdown 原生
- 本地优先
- 可读可迁移
- Git / Syncthing / 云同步更灵活
- 更适合长期技术资产沉淀

#### 8.2 数据库
建议后端主数据库选 **MySQL**。

理由：

- 与 Java / Spring Boot / Maven 组合成熟
- 你熟悉
- 足够支持该系统阶段性演进

#### 8.3 Notion 的定位
Notion 只作为后续阶段的展示层或辅助数据库，不作为第一阶段主资产层。

---

### 9. Main Interaction Model 主要交互模型

#### 9.1 Project-first Interaction
用户进入系统后，默认先进入某个项目，而不是一个没有上下文的全局聊天页。

#### 9.2 Chat Mode
适合：

- 普通问答
- 简短整理
- 单步查询
- 命令触发

#### 9.3 Plan Mode
适合：

- 知识专题整理
- 服务器排障
- 多步骤控制任务
- 批量知识卡片生成
- 工具组合调用

Plan Mode 标准流程：

1. 理解目标  
2. 收集上下文  
3. 列出约束  
4. 形成计划  
5. 等待确认或自动执行  
6. 执行并记录  
7. 输出资产  

#### 9.4 Slash Commands
Slash Commands 是正式入口，不是附加技巧。

建议第一批命令：

- `/project-new`
- `/project-switch`
- `/project-status`
- `/plan`
- `/plan-run`
- `/save-card`
- `/memory-show`
- `/memory-save`
- `/skill-list`
- `/tool-list`
- `/node-status`
- `/logs`

---

### 10. Skills / MCP / External Capabilities 外部能力策略

#### 10.1 Skills 定位
Skills 不是散落 prompt，而是正式能力包。

Skill 应包含：

- metadata
- instruction
- resources
- trigger mode
- tool bindings
- version

#### 10.2 MCP 定位
MCP 不作为系统本体，而是 Tool Gateway 中的一类工具来源。

阶段策略：

- Phase 1：预留扩展位
- Phase 2：正式接入 MCP client 与工具发现/调用
- Phase 3：形成外部能力吸收与内化闭环

#### 10.3 外部能力分类
系统应能接入的外部增强源包括：

- Skills
- MCP Tool Servers
- Knowledge Sources
- Channel Adapters（例如 Feishu）

#### 10.4 内化原则
凡是高频有效的外部能力，都应尽量沉淀为你自己的：

- internal skill
- workflow
- template
- structured note

---

### 11. Node Control Strategy 节点控制策略

#### 11.1 节点定位
本地电脑与服务器不只是被监控对象，也是能力提供者。

#### 11.2 阶段策略
Phase 1 只做只读状态：

- CPU
- 内存
- 磁盘
- 心跳
- 基础服务状态

Phase 2 开始进入受控执行：

- 查看日志
- 查看 Docker 状态
- 查看服务状态
- 重启服务
- 预定义模板命令

Phase 3 再做更强的自动化协作与运维工作流。

#### 11.3 风险控制
严禁一开始就给模型任意 shell 执行能力。  
必须采用：

- 预定义操作
- 模板命令
- 风险分级
- 人工确认机制
- 执行审计

---

### 12. Memory Strategy 记忆策略

建议采用三层正式记忆：

#### 12.1 Global Memory
全局长期偏好与固定规则。

例如：

- 输出风格偏好
- 卡片格式规则
- 技术栈偏好
- 命名规范

#### 12.2 Project Memory
项目内长期有效的上下文。

例如：

- 项目目标
- 默认节点
- 默认知识目录
- 默认 skills
- 最近关键决策

#### 12.3 Derived Memory
从已确认资产中提炼出来的高价值长期记忆。

例如：

- 运维经验规律
- 经常复用的知识结构
- 已确认的方法论

原则：

- 不是所有聊天都进入长期记忆
- 记忆必须可见、可编辑、可删除
- 项目记忆与全局记忆隔离
- 已确认资产优先级高于原始对话

---

### 13. Three-phase Roadmap 三阶段路线图

## Phase 1：Project-native Core
目标：先做对骨架，而不是先堆功能。

范围：

- Projects
- Conversations
- Global Memory
- Project Memory
- Plan Mode
- Slash Commands
- Skills 基础目录与对象模型
- 知识卡片生成
- Obsidian 写入
- 本地电脑 / 服务器只读状态

验收标准：

- 支持项目级会话管理
- 支持基础长期记忆管理
- 支持 `/plan`
- 支持至少 5 个 slash commands
- 能生成并写入知识卡片到 Obsidian
- 能查看两个节点的基础状态
- 至少连续使用并沉淀 20 张你认可的知识卡片

明确不做：

- 任意命令执行
- 自动部署
- 微信接入
- 多 agent 自治
- Notion 双写
- 重型 workflow engine

## Phase 2：Tool & Control Platform
目标：让系统从工作台升级为控制台。

范围：

- Tool Gateway 正式化
- 节点受控执行
- MCP client
- 技能生命周期管理
- 飞书轻入口
- 运维动作与知识沉淀闭环
- 数据同步方案稳定化

验收标准：

- 能通过系统执行预定义节点操作
- 能接入至少一个 MCP server
- 内置 tools 与 MCP tools 被统一建模
- 飞书支持状态查询与部分命令触发
- 本地和服务器节点 agent 具备标准化工具暴露能力

## Phase 3：Personal AI OS
目标：让系统具备持续吸收与内化外部能力的能力。

范围：

- Derived Memory
- 自动化 workflows
- Plan Library
- 英语学习模块
- Project Dashboard 强化
- 外部能力吸收 → 内部沉淀闭环
- Notion 作为可选展示层

验收标准：

- 至少有 3 条稳定工作流
- 至少有 1 条“外部能力 → 内部沉淀”的闭环
- 能统计 skills / MCP tools / workflows 的使用情况
- 系统成为默认长期 AI 工作台
- 资产、节点、学习内容在同一系统内稳定闭环

---

### 14. Phase 1 MVP Boundary 第一阶段边界

第一阶段 MVP 必须包含：

1. Projects  
2. Conversations  
3. Global / Project Memory  
4. Plan Mode  
5. Slash Commands  
6. 3 个基础 Skills  
7. Obsidian 主资产写入  
8. Nodes 只读状态  

第一阶段不应包含：

- 自由自治 agent
- 任意 shell
- 微信控制
- Notion 主存储
- 复杂插件市场
- 多模型复杂路由
- 全自动 workflow engine

---

### 15. Recommended Initial Skills 推荐首批 Skills

建议第一批只做 3 个：

#### 15.1 knowledge-card-generator
作用：

- 把对话或材料提炼成高质量知识卡片

#### 15.2 ops-summary-generator
作用：

- 把节点状态、日志、命令结果转成运维总结

#### 15.3 obsidian-note-writer
作用：

- 按固定模板写入 Obsidian 仓库

---

### 16. Suggested Initial Projects 推荐首批项目

建议第一阶段创建 3 个项目：

#### 16.1 Knowledge Base
用于技术知识沉淀与知识卡片整理。

#### 16.2 Ops Console
用于本地电脑与服务器状态查看、日志分析、运维记录。

#### 16.3 English Lab
先预留，用于后续英语学习辅助。

---

### 17. UI Information Architecture 界面信息架构

#### 左侧 Sidebar
- New Chat
- New Project
- Command Palette
- Projects 列表
- 每个 Project 下的 Conversations
- Memory
- Skills
- MCP
- Nodes
- Settings

#### 中间主区域
- Chat View
- Plan View
- Project Dashboard
- Asset View
- Node View
- Skill View
- Memory View

#### 右侧 Context Panel
动态展示：

- 当前项目记忆
- 当前启用 skills
- 当前可用 tools
- 当前 plan
- 关联 nodes
- 关联 assets

---

### 18. Final Recommendation 最终建议

Loom 的正确落地路径不是：

- 先做一个普通聊天页  
也不是  
- 先做一个很重的多智能体平台  

而是：

> 先做一个项目原生、记忆分层、计划优先、命令驱动、资产可沉淀、节点可管理、外部能力可吸收的 Personal AI OS 核心。

这是最稳、最可落地、也最适合长期演进的方案。

---

### 19. Naming Notes 命名补充

如果你后面想要更正式一点的品牌层次，可以这样用：

- 产品名：**Loom**
- 中文代号：**织机**
- 仓库：`loom`
- 后端：`loom-server`
- 前端：`loom-web`
- 节点代理：`loom-node`
- 文档仓库：`loom-docs`

如果你想保留一个更偏 AI 的副标题，可以写成：

**Loom — Personal AI OS for Projects, Memory, Plans and Nodes**
