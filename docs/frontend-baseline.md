# 前端工作台与中文化基线
更新时间：2026-04-06

## 桌面端工作台基线

- 电脑端默认必须是全屏工作台，不使用居中的 `max-width` 展示页外壳。
- 桌面端采用四段式信息结构：
  `Global Rail`、`Project Pane`、`Main Canvas`、`Inspector`。
- `Project Pane` 必须承担真正的项目级会话管理职责：
  项目切换、会话分组、多轮切换、重命名、归档、搜索、筛选。
- `Main Canvas` 必须提供真实页面，而不是空壳或占位：
  `Project Home`、`Conversation Workspace`、`Plan Workspace`、`Memory Center`、`Assets Library`、`Nodes Center`、`Settings Center`。
- `Inspector` 必须持续展示当前上下文：
  当前项目、关键记忆、关联资产、当前 Plan 摘要、节点和技能。
- 管理型页面优先使用列表、表格、行项详情，不堆叠大面积营销式卡片。

## 文案语言基线

- 普通用户可见文案默认使用中文。
- 保留英文的专业术语包括：
  `Chat`、`Plan`、`Plan Mode`、`Memory`、`Assets`、`Nodes`、`Obsidian`、`API`、`Vault`、`Command`、`Skill`、`Provider`。
- Slash Commands 本身保留英文原样，例如 `/plan`、`/save-card`。
- 后端 bootstrap、settings、mock 数据、默认命令反馈，也必须遵守中文优先规则。

## 视觉与交互基线

- 视觉方向固定为：
  冷灰、石墨、蓝绿点缀，避免暖色宣传页气质。
- 字体固定为：
  `Geist Sans` + `JetBrains Mono`。
- 图标固定为：
  `lucide-react` 单一图标体系。
- 图标按钮、筛选按钮、输入框、对话框必须使用统一尺寸和圆角体系，不允许出现碎片化风格。
- 设置页必须是真实控制中心，不允许再出现“空白说明页”。

## 当前验收点

- 打开 Loom 后，桌面端应直接进入全屏工作台。
- 左侧必须能看到项目切换和项目级会话管理。
- 至少可以看到多轮会话切换、Plan 工作区、Memory、Assets、Nodes、Settings。
- 除专业术语外，按钮、说明、反馈、默认标题应为中文。
- 新版浏览器标题必须为 `Loom 工作台`。

## 2026-04-06 本轮落地

- 已完成项目级工作台重构，替换旧的单页拼装式界面。
- 已补齐项目级会话管理：
  新建 Chat、新建 Plan、切换、多轮浏览、重命名、归档、搜索、模式筛选。
- 已补齐真实设置中心：
  工作区、项目默认项、模型与 Provider、Vault、Nodes、Commands / Skills、诊断与关于。
- 已统一中文文案，并清理共享 contracts、mock 数据和前端源码中的编码污染。

## 后续验收关注点

- 布局的专业感、高级感是否达到预期。
- 信息密度是否仍需进一步收口。
- 图标按钮、表格排版、行高和留白是否还需要第二轮精修。
