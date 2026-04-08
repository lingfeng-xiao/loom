package com.loom.server.service;

import com.loom.server.config.LoomServerProperties;
import com.loom.server.model.BootstrapPayload;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LoomBootstrapService {

    private final LoomServerProperties serverProperties;

    public LoomBootstrapService(LoomServerProperties serverProperties) {
        this.serverProperties = serverProperties;
    }

    public BootstrapPayload getBootstrap() {
        return new BootstrapPayload(
                coalesce(serverProperties.getAppName(), "loom"),
                coalesce(serverProperties.getDescription(), "这是一个带有可见 Trace 和可配置能力壳层的项目化 AI 会话工作台。"),
                new BootstrapPayload.ProjectSummary(
                        "project-loom",
                        "loom",
                        "Phase 1 启动阶段",
                        "这是一个以会话为中心的工作台，用来打磨壳层、冻结合同边界，并为后续执行链路做准备。",
                        "项目：loom",
                        "2 分钟前更新",
                        "已连接"
                ),
                List.of(
                        new BootstrapPayload.WorkspacePageLink("conversation", "会话", "会话工作台", "Cmd+1", true),
                        new BootstrapPayload.WorkspacePageLink("capabilities", "能力", "Models、MCP、Skills 和执行器", "Cmd+2", true),
                        new BootstrapPayload.WorkspacePageLink("openclaw", "OpenClaw", "外部执行器可见性", "Cmd+3", true),
                        new BootstrapPayload.WorkspacePageLink("files", "Files", "项目文件池", "Cmd+4", false),
                        new BootstrapPayload.WorkspacePageLink("memory", "Memory", "分层 Memory 工作区", "Cmd+5", false),
                        new BootstrapPayload.WorkspacePageLink("settings", "设置", "分层配置", "Cmd+6", true)
                ),
                List.of(
                        new BootstrapPayload.ConversationSummary(
                                "conversation-shell",
                                "工作台壳层实现",
                                "对齐三栏布局、消息流，以及 Trace / Context 面板的表现。",
                                "2 分钟前",
                                "chat",
                                "active",
                                false
                        ),
                        new BootstrapPayload.ConversationSummary(
                                "conversation-openclaw",
                                "OpenClaw 路由复盘",
                                "复核外部执行器发现、路由策略和运行可见性。",
                                "20 分钟前",
                                "plan",
                                "idle",
                                false
                        ),
                        new BootstrapPayload.ConversationSummary(
                                "conversation-contract",
                                "Phase 1 合同基线",
                                "冻结会话壳层和占位页面所需的最小读取模型。",
                                "1 小时前",
                                "review",
                                "blocked",
                                false
                        )
                ),
                List.of(
                        new BootstrapPayload.ConversationSummary(
                                "conversation-v1",
                                "loom V1 定义",
                                "Kickoff 阶段的产品定义、范围边界和角色分工。",
                                "已置顶",
                                "plan",
                                "active",
                                true
                        ),
                        new BootstrapPayload.ConversationSummary(
                                "conversation-trace",
                                "Trace 体验",
                                "让系统过程长期可见，而不是把所有状态变化都塞进消息流。",
                                "已置顶",
                                "review",
                                "idle",
                                true
                        )
                ),
                List.of(
                        new BootstrapPayload.WorkspaceModeOption("chat", "Chat", "适合开放式推进和自由讨论。"),
                        new BootstrapPayload.WorkspaceModeOption("plan", "Plan", "突出目标、约束和候选方案。"),
                        new BootstrapPayload.WorkspaceModeOption("action", "Action", "突出任务触发和执行可见性。"),
                        new BootstrapPayload.WorkspaceModeOption("review", "Review", "用于记录复盘、纠偏和结论。")
                ),
                "chat",
                "会话系统 V1 设计",
                "项目：loom | 进行中 | 优先完成 UI 壳层",
                List.of(
                        new BootstrapPayload.ConversationMessage(
                                "message-user",
                                "user",
                                "用户",
                                "先把 loom 工作台壳层做成可运行、可评审、并且便于后续接线的状态。",
                                null,
                                null
                        ),
                        new BootstrapPayload.ConversationMessage(
                                "message-thinking",
                                "thinking_summary",
                                "思考摘要",
                                "先对齐三栏布局，再统一会话、能力、OpenClaw 和设置页面的壳层结构。",
                                "以会话为中心 | Trace 可见 | 先完成壳层再进入完整实现",
                                null
                        ),
                        new BootstrapPayload.ConversationMessage(
                                "message-assistant",
                                "assistant",
                                "助手",
                                "这一轮先冻结 UI 壳层、最小合同和测试基线，避免后续联调边做边漂移。",
                                "关键页面：会话 / 能力 / OpenClaw / 设置",
                                null
                        ),
                        new BootstrapPayload.ConversationMessage(
                                "message-action",
                                "action_card",
                                "动作卡片",
                                "已经对齐 docs/frontend 中的 tokens、API client、路由规划和组件架构输入。",
                                null,
                                "已完成"
                        )
                ),
                new BootstrapPayload.ComposerState(
                        "描述下一步想让 loom 协调的设计或交付事项。",
                        "发送",
                        List.of("挂载 Context", "挂载文件", "打开命令面板"),
                        List.of(
                                new BootstrapPayload.ComposerToggle("允许动作", true),
                                new BootstrapPayload.ComposerToggle("Memory", true),
                                new BootstrapPayload.ComposerToggle("上传", false)
                        )
                ),
                "当前壳层会先读取 kickoff 文档和 docs/frontend 输入，再把 Phase 1 收敛到路由页面、可见 Trace 输出和最小 bootstrap 合同。",
                List.of(
                        new BootstrapPayload.TraceStep("trace-context", "读取前端文档输入", "tokens + sdk + router + component contracts", "success"),
                        new BootstrapPayload.TraceStep("trace-contract", "冻结壳层合同", "bootstrap 读取模型", "success"),
                        new BootstrapPayload.TraceStep("trace-shell", "实现路由化壳层", "conversation / capabilities / openclaw / settings", "running"),
                        new BootstrapPayload.TraceStep("trace-docs", "部署验收环境", "backend + web preview", "pending")
                ),
                List.of(
                        new BootstrapPayload.ContextBlock("context-goal", "当前目标", "交付 Phase 1 UI 壳层，并冻结最小 bootstrap 合同。"),
                        new BootstrapPayload.ContextBlock("context-constraints", "约束条件", "保持会话优先、项目壳层轻量、Trace 可见，并强制维护文档。"),
                        new BootstrapPayload.ContextBlock("context-summary", "摘要", "这一轮只聚焦壳层，不进入完整消息生成、Memory 持久化或动作执行。"),
                        new BootstrapPayload.ContextBlock("context-active", "进行中的事项", "三栏布局、页面切换、界面状态表现，以及统一的合同基线。"),
                        new BootstrapPayload.ContextBlock("context-files", "参考输入", "设计方案、三个 HTML 原型，以及 docs/frontend 下的 API、token、路由和组件文档。"),
                        new BootstrapPayload.ContextBlock("context-open", "未闭环事项", "真实后端数据、回调链路，以及 Files / Memory 的后续实现。")
                ),
                new BootstrapPayload.CapabilitiesOverview(
                        "在不展开每个细节配置页的前提下，展示当前系统能力栈和项目绑定关系。",
                        List.of(
                                new BootstrapPayload.OverviewCard("cap-models", "Models", "面向聊天、规划和摘要流程的默认模型配置。",
                                        List.of("GPT-5.4 Thinking", "已开启流式输出", "支持长上下文")),
                                new BootstrapPayload.OverviewCard("cap-mcp", "MCP Servers", "当前工作台可用的资源、提示词和工具提供方。",
                                        List.of("local-dev", "notion-mcp", "internal-hub")),
                                new BootstrapPayload.OverviewCard("cap-skills", "Skills", "当前项目已经启用的核心工作流能力。",
                                        List.of("planning", "summarize", "retrieve-context")),
                                new BootstrapPayload.OverviewCard("cap-executors", "执行器", "内部执行与外部执行器路由的组合方式。",
                                        List.of("内部执行", "OpenClaw"))
                        ),
                        List.of(
                                new BootstrapPayload.StatusItem("默认聊天模型", "GPT-5.4 Thinking", "accent"),
                                new BootstrapPayload.StatusItem("异步外部任务", "OpenClaw 执行器", "good"),
                                new BootstrapPayload.StatusItem("普通聊天回复", "内部 pipeline", "neutral")
                        )
                ),
                new BootstrapPayload.OpenClawOverview(
                        "把 OpenClaw 作为可见、可控的执行桥接层，而不是不可观察的黑盒依赖。",
                        List.of(
                                new BootstrapPayload.DetailItem("Gateway 地址", "http://127.0.0.1:18789"),
                                new BootstrapPayload.DetailItem("状态", "已连接"),
                                new BootstrapPayload.DetailItem("最近心跳", "12 秒前")
                        ),
                        List.of(
                                new BootstrapPayload.StatusItem("通道", "3", "accent"),
                                new BootstrapPayload.StatusItem("工具", "12", "good"),
                                new BootstrapPayload.StatusItem("Skills", "6", "accent"),
                                new BootstrapPayload.StatusItem("插件", "5", "neutral")
                        ),
                        List.of(
                                new BootstrapPayload.StatusItem("异步任务", "OpenClaw", "good"),
                                new BootstrapPayload.StatusItem("飞书桥接", "OpenClaw", "good"),
                                new BootstrapPayload.StatusItem("普通聊天回复", "内部执行", "warn")
                        ),
                        List.of(
                                new BootstrapPayload.StatusItem("run_001", "success", "good"),
                                new BootstrapPayload.StatusItem("run_002", "waiting", "warn"),
                                new BootstrapPayload.StatusItem("run_003", "failed", "danger")
                        ),
                        List.of("OpenClaw 执行复盘", "路由策略回顾")
                ),
                new BootstrapPayload.SettingsOverview(
                        "通过分栏和上下文说明来管理全局、项目和会话范围内的设置。",
                        List.of("Models", "Skills", "MCP", "Memory", "Routing"),
                        List.of(
                                new BootstrapPayload.DetailItem("配置名称", "GPT-5.4 Thinking"),
                                new BootstrapPayload.DetailItem("提供方", "OpenAI-compatible"),
                                new BootstrapPayload.DetailItem("模型 ID", "gpt-5.4-thinking"),
                                new BootstrapPayload.DetailItem("能力", "Streaming | Images | Tools | Long Context"),
                                new BootstrapPayload.DetailItem("超时", "30000 ms")
                        ),
                        List.of(
                                "Global、Project 和 Conversation 三层配置范围必须保持清晰。",
                                "长会话默认要同时保留摘要和最近的原始消息。",
                                "异步外部任务默认走 OpenClaw，普通聊天回复继续走内部 pipeline。"
                        ),
                        List.of(
                                "启用自动外部动作前，需要再次确认路由策略。",
                                "外部回调必须补齐状态校验和超时处理。",
                                "任何配置变更都必须同步到运行和测试文档里。"
                        )
                )
        );
    }

    private String coalesce(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
