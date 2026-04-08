package com.loom.server.workspace;

import com.loom.server.api.ApiException;
import com.loom.server.model.BootstrapPayload;
import com.loom.server.workspace.WorkspaceDtos.CapabilityBindingSummary;
import com.loom.server.workspace.WorkspaceDtos.CapabilityBindingRuleView;
import com.loom.server.workspace.WorkspaceDtos.CapabilityCardView;
import com.loom.server.workspace.WorkspaceDtos.CapabilityOverviewView;
import com.loom.server.workspace.WorkspaceDtos.ActionView;
import com.loom.server.workspace.WorkspaceDtos.ContextPanelView;
import com.loom.server.workspace.WorkspaceDtos.ContextReferenceItem;
import com.loom.server.workspace.WorkspaceDtos.ContextRefreshResponse;
import com.loom.server.workspace.WorkspaceDtos.ContextSnapshotView;
import com.loom.server.workspace.WorkspaceDtos.ConversationListItem;
import com.loom.server.workspace.WorkspaceDtos.ConversationView;
import com.loom.server.workspace.WorkspaceDtos.CreateConversationRequest;
import com.loom.server.workspace.WorkspaceDtos.CursorPage;
import com.loom.server.workspace.WorkspaceDtos.FileAssetSummaryView;
import com.loom.server.workspace.WorkspaceDtos.McpServerView;
import com.loom.server.workspace.WorkspaceDtos.MemoryItemView;
import com.loom.server.workspace.WorkspaceDtos.MemoryPolicyView;
import com.loom.server.workspace.WorkspaceDtos.MessageView;
import com.loom.server.workspace.WorkspaceDtos.ModelProfileView;
import com.loom.server.workspace.WorkspaceDtos.ProjectListItem;
import com.loom.server.workspace.WorkspaceDtos.ProjectView;
import com.loom.server.workspace.WorkspaceDtos.RoutingPolicyView;
import com.loom.server.workspace.WorkspaceDtos.RunStepView;
import com.loom.server.workspace.WorkspaceDtos.RunView;
import com.loom.server.workspace.WorkspaceDtos.SettingsOverviewView;
import com.loom.server.workspace.WorkspaceDtos.SkillView;
import com.loom.server.workspace.WorkspaceDtos.SubmitMessageRequest;
import com.loom.server.workspace.WorkspaceDtos.SubmitMessageResponse;
import com.loom.server.workspace.WorkspaceDtos.TracePanelView;
import com.loom.server.workspace.WorkspaceDtos.UpdateConversationRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class WorkspaceStateService {
    private final AtomicInteger conversationSeq = new AtomicInteger(5);
    private final AtomicInteger messageSeq = new AtomicInteger(10);
    private final ProjectState project = new ProjectState();
    private final LinkedHashMap<String, ConversationState> conversations = new LinkedHashMap<>();
    private final LinkedHashMap<String, ActionState> actions = new LinkedHashMap<>();
    private final List<FileAssetSummaryView> fileAssets = List.of(
            new FileAssetSummaryView("file-prd", "project-loom", "loom-prd.md", "text/markdown", 18_240, "ready", now()),
            new FileAssetSummaryView("file-contract", "project-loom", "phase1-contract-freeze.md", "text/markdown", 24_512, "ready", now()),
            new FileAssetSummaryView("file-smoke", "project-loom", "frontend-smoke-checklist.md", "text/markdown", 6_144, "pending", now())
    );
    private final List<MemoryItemView> memoryItems = List.of(
            new MemoryItemView("memory-project-goal", "project", "project-loom", null, "保持会话优先、Trace 可见和文档先行。", "explicit", now()),
            new MemoryItemView("memory-contract", "project", "project-loom", null, "合同冻结后优先保障 REST / SSE 字段口径不漂移。", "system", now()),
            new MemoryItemView("memory-conversation-v1", "conversation", "project-loom", "conversation-v1", "当前会话聚焦 Context、Settings、Capabilities 与联调闭环。", "assisted", now())
    );
    private final SettingsOverviewView settings = new SettingsOverviewView(
            "project",
            List.of("Models", "Skills", "MCP", "Memory", "Routing"),
            List.of(new ModelProfileView("model-gpt54-thinking", "project", "GPT-5.4 Thinking", "OpenAI-compatible", "gpt-5.4-thinking", true, true, true, true, true, 30000)),
            List.of(
                    new SkillView("skill-planning", "project", "planning", true, "internal"),
                    new SkillView("skill-summarize", "project", "summarize", true, "internal"),
                    new SkillView("skill-retrieve-context", "project", "retrieve-context", true, "internal")
            ),
            List.of(
                    new McpServerView("mcp-local-dev", "project", "local-dev", "connected", 4, 2, 3),
                    new McpServerView("mcp-notion", "project", "notion-mcp", "connected", 6, 1, 0)
            ),
            new MemoryPolicyView("memory-default", "project", true, true, true),
            new RoutingPolicyView("routing-default", "project", "internal", true, "OpenClaw")
    );

    public WorkspaceStateService() {
        seedConversation("conversation-v1", "loom V1 定义", "Kickoff 阶段的产品定义、范围边界和角色分工。", "plan", "active", true);
        seedConversation("conversation-trace", "Trace 体验", "让系统过程长期可见，而不是把所有状态变化都塞进消息流。", "review", "idle", true);
        seedConversation("conversation-shell", "工作台壳层实现", "对齐三栏布局、消息流，以及 Trace / Context 面板的表现。", "chat", "active", false);
        conversations.get("conversation-v1").messages.add(newMessage("conversation-v1", "user", "user", "先把 loom 工作台壳层做成可运行、可评审、并且便于后续接线的状态。", null));
        conversations.get("conversation-v1").messages.add(newMessage("conversation-v1", "thinking_summary", "assistant", "先对齐三栏布局，再统一会话、能力、OpenClaw 和设置页面的壳层结构。", "以会话为中心 | Trace 可见 | 先完成壳层再进入完整实现"));
        conversations.get("conversation-v1").messages.add(newMessage("conversation-v1", "assistant", "assistant", "这一轮先冻结 UI 壳层、最小合同和测试基线，避免后续联调边做边漂移。", "关键页面：会话 / 能力 / OpenClaw / 设置"));
    }

    public CursorPage<ProjectListItem> listProjects() {
        return new CursorPage<>(List.of(new ProjectListItem(project.id, project.name, project.description, project.status, conversations.size(), project.lastMessageAt, project.updatedAt)), null, false);
    }

    public ProjectView getProject(String projectId) {
        requireProject(projectId);
        return new ProjectView(project.id, project.name, project.description, project.status, conversations.size(), project.lastMessageAt, project.updatedAt, project.instructions, project.pinnedConversationIds, project.bindings);
    }

    public CursorPage<ConversationListItem> listConversations(String projectId) {
        requireProject(projectId);
        return new CursorPage<>(conversations.values().stream().sorted(Comparator.comparing((ConversationState c) -> c.updatedAt).reversed()).map(this::toConversationListItem).toList(), null, false);
    }

    public ConversationView createConversation(String projectId, CreateConversationRequest request) {
        requireProject(projectId);
        String id = "conversation-" + conversationSeq.incrementAndGet();
        ConversationState state = new ConversationState(id, projectId, blankTo(request == null ? null : request.title(), "新会话"), blankTo(request == null ? null : request.mode(), "chat"), false);
        initializeContext(state);
        conversations.put(id, state);
        return toConversationView(state);
    }

    public ConversationView getConversation(String projectId, String conversationId) {
        requireProject(projectId);
        return toConversationView(requireConversation(conversationId));
    }

    public ConversationView updateConversation(String projectId, String conversationId, UpdateConversationRequest request) {
        requireProject(projectId);
        ConversationState state = requireConversation(conversationId);
        if (request != null) {
            state.title = blankTo(request.title(), state.title);
            state.mode = blankTo(request.mode(), state.mode);
            state.status = blankTo(request.status(), state.status);
            state.pinned = request.pinned() != null ? request.pinned() : state.pinned;
            state.updatedAt = now();
        }
        return toConversationView(state);
    }

    public CursorPage<MessageView> listMessages(String projectId, String conversationId) {
        requireProject(projectId);
        return new CursorPage<>(List.copyOf(requireConversation(conversationId).messages), null, false);
    }

    public SubmitMessageResponse submitMessage(String projectId, String conversationId, SubmitMessageRequest request) {
        requireProject(projectId);
        if (request == null || request.body() == null || request.body().isBlank()) throw new IllegalArgumentException("Message body is required");
        ConversationState state = requireConversation(conversationId);
        MessageView user = newMessage(conversationId, "user", "user", request.body().trim(), null);
        MessageView thinking = newMessage(conversationId, "thinking_summary", "assistant", "正在整理目标、上下文和下一步动作。", "已根据最新消息刷新上下文和执行摘要。");
        MessageView assistant = newMessage(conversationId, "assistant", "assistant", "已收到你的最新需求，当前会优先刷新会话、Trace、Context 和设置面板对应的数据读取链路。", "真实主数据已更新，可继续进入下一步联调。");
        ActionState action = newAction(state, request.body().trim(), assistant.completedAt());
        state.messages.add(user);
        state.messages.add(thinking);
        state.messages.add(assistant);
        state.summary = abbreviate(request.body().trim());
        state.status = "active";
        state.updatedAt = assistant.completedAt();
        state.lastMessageAt = assistant.completedAt();
        state.activeActionId = action.id;
        state.activeRunId = action.runId;
        updateContextAfterMessage(state, request.body().trim(), assistant.completedAt());
        project.updatedAt = state.updatedAt;
        project.lastMessageAt = state.lastMessageAt;
        return new SubmitMessageResponse(conversationId, user, state.activeRunId, "/api/projects/" + projectId + "/conversations/" + conversationId + "/stream");
    }

    public ContextPanelView getContext(String projectId, String conversationId) {
        requireProject(projectId);
        ConversationState state = requireConversation(conversationId);
        return contextFor(state);
    }

    public ContextRefreshResponse refreshContext(String projectId, String conversationId) {
        requireProject(projectId);
        ConversationState state = requireConversation(conversationId);
        String refreshedAt = now();
        state.context.refreshCount++;
        state.context.updatedAt = refreshedAt;
        state.context.conversationSummary = "已基于最新会话与项目绑定信息刷新上下文，准备进入下一轮实现与联调。";
        state.context.decisions = List.of(
                "优先保持会话主链路可用",
                "Context 优先输出对当前开发最有价值的摘要",
                "刷新结果 v" + state.context.refreshCount + " 已覆盖目标与风险"
        );
        state.context.openLoops = List.of(
                "补齐 Context 页面真数据接线",
                "补齐 Settings / Capabilities 页面真数据读取",
                "形成联调记录与 go / no-go 结论"
        );
        state.context.activeGoals = List.of(
                "推进真实主链路",
                "完成第 " + state.context.refreshCount + " 次上下文刷新"
        );
        state.context.references = List.of(
                new ContextReferenceItem("ref-contract-freeze", "合同冻结", "file", "Phase 1 合同冻结、事件名和模块边界"),
                new ContextReferenceItem("ref-recent-message", "最新用户消息", "conversation", latestUserSummary(state))
        );
        state.context.snapshots = appendSnapshot(state.context.snapshots, new ContextSnapshotView(
                "snapshot-active-context-" + UUID.randomUUID(),
                project.id,
                conversationId,
                "active_context",
                state.context.conversationSummary,
                refreshedAt
        ));
        state.updatedAt = refreshedAt;
        return new ContextRefreshResponse(contextFor(state));
    }

    public TracePanelView getTrace(String projectId, String conversationId) {
        requireProject(projectId);
        ConversationState state = requireConversation(conversationId);
        ActionView action = state.activeActionId == null ? null : actionView(state.activeActionId);
        RunView run = state.activeRunId == null ? null : new RunView(state.activeRunId, action == null ? "action-" + state.activeRunId : action.id(), projectId, conversationId, "success", state.updatedAt, state.updatedAt, null);
        List<RunStepView> steps = List.of(
                new RunStepView("trace-context-" + conversationId, state.activeRunId, "读取上下文", "读取会话、设置与目标摘要", "success", state.updatedAt, state.updatedAt, null),
                new RunStepView("trace-reply-" + conversationId, state.activeRunId, "生成回复", "生成思考摘要与回复内容", state.activeRunId == null ? "running" : "success", state.updatedAt, state.activeRunId == null ? null : state.updatedAt, null),
                new RunStepView("trace-followup-" + conversationId, state.activeRunId, "准备后续联调", "等待下一次主链路联调或刷新", state.activeRunId == null ? "pending" : "success", state.updatedAt, state.activeRunId == null ? null : state.updatedAt, null)
        );
        return new TracePanelView("当前会先读取前端文档输入，再把 Phase 1 收敛到可接线状态。", action, run, steps, state.updatedAt);
    }

    public RunView getRun(String projectId, String conversationId, String runId) {
        TracePanelView trace = getTrace(projectId, conversationId);
        if (trace.activeRun() == null || !trace.activeRun().id().equals(runId)) throw new ApiException(HttpStatus.NOT_FOUND, "RUN_NOT_FOUND", "Run does not exist");
        return trace.activeRun();
    }

    public ActionView getAction(String projectId, String conversationId, String actionId) {
        requireProject(projectId);
        requireConversation(conversationId);
        ActionView action = actionView(actionId);
        if (!action.projectId().equals(projectId) || !action.conversationId().equals(conversationId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ACTION_NOT_FOUND", "Action does not exist");
        }
        return action;
    }

    public CursorPage<RunStepView> listRunSteps(String projectId, String conversationId, String runId) {
        requireProject(projectId);
        TracePanelView trace = getTrace(projectId, conversationId);
        if (trace.activeRun() == null || !trace.activeRun().id().equals(runId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "RUN_NOT_FOUND", "Run does not exist");
        }
        return new CursorPage<>(trace.steps(), null, false);
    }

    public CursorPage<FileAssetSummaryView> listFiles(String projectId) {
        requireProject(projectId);
        return new CursorPage<>(fileAssets, null, false);
    }

    public CursorPage<MemoryItemView> listMemory(String projectId) {
        requireProject(projectId);
        return new CursorPage<>(memoryItems, null, false);
    }

    public SettingsOverviewView getSettingsOverview(String scope) {
        if (scope == null || scope.isBlank() || scope.equals(settings.activeScope())) return settings;
        return new SettingsOverviewView(scope, settings.tabs(), settings.modelProfiles(), settings.skills(), settings.mcpServers(), settings.memoryPolicy(), settings.routingPolicy());
    }

    public CapabilityOverviewView getCapabilitiesOverview(String scope) {
        SettingsOverviewView scopedSettings = getSettingsOverview(scope);
        return new CapabilityOverviewView(
                scopedSettings.activeScope(),
                "通过统一能力摘要展示当前模型、Skills、MCP 与执行策略绑定关系。",
                List.of(
                        new CapabilityCardView("cap-models", "Models", "当前作用域默认模型与能力支持。", scopedSettings.modelProfiles().stream().map(ModelProfileView::name).toList()),
                        new CapabilityCardView("cap-skills", "Skills", "当前作用域启用的技能。", scopedSettings.skills().stream().map(SkillView::name).toList()),
                        new CapabilityCardView("cap-mcp", "MCP Servers", "当前作用域已连接的资源与工具提供方。", scopedSettings.mcpServers().stream().map(McpServerView::name).toList()),
                        new CapabilityCardView("cap-executors", "Executors", "执行运行时与外部执行器入口。", List.of(scopedSettings.routingPolicy().defaultRuntime(), blankTo(scopedSettings.routingPolicy().externalExecutorLabel(), "none")))
                ),
                List.of(
                        new CapabilityBindingRuleView("默认聊天模型", project.bindings.defaultModelProfileId(), "accent"),
                        new CapabilityBindingRuleView("启用技能数", String.valueOf(project.bindings.enabledSkillIds().size()), "good"),
                        new CapabilityBindingRuleView("默认路由策略", project.bindings.defaultRoutingPolicyId(), "neutral")
                )
        );
    }

    public List<Map<String, Object>> getStreamEvents(String projectId, String conversationId) {
        requireProject(projectId);
        ConversationState state = requireConversation(conversationId);
        String emittedAt = now();
        List<Map<String, Object>> events = new ArrayList<>();
        MessageView thinking = latestMessageByKind(state, "thinking_summary");
        MessageView assistant = latestMessageByKind(state, "assistant");
        TracePanelView trace = getTrace(projectId, conversationId);

        appendMessageDeltaEvents(events, "thinking.summary.delta", projectId, conversationId, emittedAt, thinking);
        if (thinking != null) {
            events.add(event("thinking.summary.done", projectId, conversationId, emittedAt, Map.of("message", thinking)));
        }

        appendMessageDeltaEvents(events, "message.delta", projectId, conversationId, emittedAt, assistant);
        if (assistant != null) {
            events.add(event("message.done", projectId, conversationId, emittedAt, Map.of("message", assistant)));
        }

        String runId = trace.activeRun() != null ? trace.activeRun().id() : "run-preview-" + conversationId;
        for (RunStepView step : trace.steps()) {
            events.add(event("trace.step.created", projectId, conversationId, emittedAt, Map.of("runId", runId, "step", step)));
            String stepEvent = switch (step.status()) {
                case "success", "failed", "skipped" -> "trace.step.completed";
                default -> "trace.step.updated";
            };
            events.add(event(stepEvent, projectId, conversationId, emittedAt, Map.of("runId", runId, "step", step)));
        }

        events.add(event("context.updated", projectId, conversationId, emittedAt, Map.of("context", getContext(projectId, conversationId))));
        if (trace.activeRun() != null) {
            String runEvent = "failed".equals(trace.activeRun().status()) ? "run.failed" : "run.completed";
            events.add(event(runEvent, projectId, conversationId, emittedAt, Map.of("run", trace.activeRun())));
        }
        return events;
    }

    public BootstrapPayload buildBootstrapPayload() {
        ConversationState active = conversations.values().stream().sorted(Comparator.comparing((ConversationState c) -> c.updatedAt).reversed()).findFirst().orElseThrow();
        ContextPanelView context = contextFor(active);
        TracePanelView trace = getTrace(project.id, active.id);
        return new BootstrapPayload(
                "loom",
                "这是一个带有可见 Trace 和可配置能力壳层的项目化 AI 会话工作台。",
                new BootstrapPayload.ProjectSummary(project.id, project.name, "Phase 1 启动阶段", project.description, "项目：" + project.name, "最近更新 " + project.updatedAt, "已连接"),
                List.of(
                        new BootstrapPayload.WorkspacePageLink("conversation", "会话", "会话工作台", "Cmd+1", true),
                        new BootstrapPayload.WorkspacePageLink("capabilities", "能力", "Models、MCP、Skills 和执行器", "Cmd+2", true),
                        new BootstrapPayload.WorkspacePageLink("openclaw", "OpenClaw", "外部执行器可见性", "Cmd+3", true),
                        new BootstrapPayload.WorkspacePageLink("files", "Files", "项目文件池", "Cmd+4", false),
                        new BootstrapPayload.WorkspacePageLink("memory", "Memory", "分层 Memory 工作区", "Cmd+5", false),
                        new BootstrapPayload.WorkspacePageLink("settings", "设置", "分层配置", "Cmd+6", true)
                ),
                conversations.values().stream().filter(c -> !c.pinned).sorted(Comparator.comparing((ConversationState c) -> c.updatedAt).reversed()).map(this::toBootstrapConversation).toList(),
                conversations.values().stream().filter(c -> c.pinned).sorted(Comparator.comparing((ConversationState c) -> c.updatedAt).reversed()).map(this::toBootstrapConversation).toList(),
                List.of(
                        new BootstrapPayload.WorkspaceModeOption("chat", "Chat", "适合开放式推进和自由讨论。"),
                        new BootstrapPayload.WorkspaceModeOption("plan", "Plan", "突出目标、约束和候选方案。"),
                        new BootstrapPayload.WorkspaceModeOption("action", "Action", "突出任务触发和执行可见性。"),
                        new BootstrapPayload.WorkspaceModeOption("review", "Review", "用于记录复盘、纠偏和结论。")
                ),
                active.mode,
                active.title,
                project.name + " | " + active.summary,
                active.messages.stream().map(m -> new BootstrapPayload.ConversationMessage(m.id(), m.kind(), "user".equals(m.role()) ? "用户" : "助手", m.body(), m.summary(), m.statusLabel())).toList(),
                new BootstrapPayload.ComposerState(
                        "描述下一步想让 loom 协调的设计或交付事项。",
                        "发送",
                        List.of("挂载 Context", "挂载文件", "打开命令面板"),
                        List.of(new BootstrapPayload.ComposerToggle("允许动作", true), new BootstrapPayload.ComposerToggle("Memory", true), new BootstrapPayload.ComposerToggle("上传", false))
                ),
                trace.reasoningSummary(),
                trace.steps().stream().map(s -> new BootstrapPayload.TraceStep(s.id(), s.title(), s.detail(), s.status())).toList(),
                List.of(
                        new BootstrapPayload.ContextBlock("context-goal", "当前目标", context.activeGoals().isEmpty() ? "推进真实主链路" : context.activeGoals().get(0)),
                        new BootstrapPayload.ContextBlock("context-constraints", "约束条件", String.join("；", context.constraints())),
                        new BootstrapPayload.ContextBlock("context-summary", "摘要", context.conversationSummary()),
                        new BootstrapPayload.ContextBlock("context-active", "进行中的事项", String.join("；", context.decisions())),
                        new BootstrapPayload.ContextBlock("context-files", "参考输入", context.references().stream().map(ContextReferenceItem::summary).findFirst().orElse("无")),
                        new BootstrapPayload.ContextBlock("context-open", "未闭环事项", String.join("；", context.openLoops()))
                ),
                new BootstrapPayload.CapabilitiesOverview(
                        "在不展开每个细节配置页的前提下，展示当前系统能力栈和项目绑定关系。",
                        List.of(
                                new BootstrapPayload.OverviewCard("cap-models", "Models", "项目默认模型配置。", settings.modelProfiles().stream().map(ModelProfileView::name).toList()),
                                new BootstrapPayload.OverviewCard("cap-mcp", "MCP Servers", "当前可用的资源和工具提供方。", settings.mcpServers().stream().map(McpServerView::name).toList()),
                                new BootstrapPayload.OverviewCard("cap-skills", "Skills", "当前项目已启用的技能。", settings.skills().stream().map(SkillView::name).toList()),
                                new BootstrapPayload.OverviewCard("cap-executors", "执行器", "内部执行与外部执行器路由。", List.of("内部执行", "OpenClaw"))
                        ),
                        List.of(
                                new BootstrapPayload.StatusItem("默认聊天模型", settings.modelProfiles().get(0).name(), "accent"),
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
                        trace.steps().stream().limit(3).map(s -> new BootstrapPayload.StatusItem(s.id(), s.status(), "success".equals(s.status()) ? "good" : "warn")).toList(),
                        conversations.values().stream().filter(c -> c.pinned).map(c -> c.title).toList()
                ),
                new BootstrapPayload.SettingsOverview(
                        "通过分栏和上下文说明来管理全局、项目和会话范围内的设置。",
                        settings.tabs(),
                        List.of(
                                new BootstrapPayload.DetailItem("配置名称", settings.modelProfiles().get(0).name()),
                                new BootstrapPayload.DetailItem("提供方", settings.modelProfiles().get(0).provider()),
                                new BootstrapPayload.DetailItem("模型 ID", settings.modelProfiles().get(0).modelId()),
                                new BootstrapPayload.DetailItem("能力", "Streaming | Images | Tools | Long Context"),
                                new BootstrapPayload.DetailItem("超时", settings.modelProfiles().get(0).timeoutMs() + " ms")
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

    private void seedConversation(String id, String title, String summary, String mode, String status, boolean pinned) {
        ConversationState state = new ConversationState(id, project.id, title, mode, pinned);
        state.summary = summary;
        state.status = status;
        initializeContext(state);
        conversations.put(id, state);
    }

    private ActionState newAction(ConversationState conversation, String summary, String completedAt) {
        String id = "action-" + UUID.randomUUID();
        String runId = "run-" + UUID.randomUUID();
        ActionState state = new ActionState(
                id,
                project.id,
                conversation.id,
                runId,
                "message-response",
                "completed",
                summary,
                completedAt,
                completedAt,
                List.of(
                        "trace-context-" + conversation.id,
                        "trace-reply-" + conversation.id,
                        "trace-followup-" + conversation.id
                )
        );
        actions.put(id, state);
        return state;
    }

    private ActionView actionView(String actionId) {
        ActionState state = actions.get(actionId);
        if (state == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ACTION_NOT_FOUND", "Action does not exist");
        }
        return state.toView();
    }

    private ConversationListItem toConversationListItem(ConversationState state) {
        return new ConversationListItem(state.id, state.projectId, state.title, state.summary, state.mode, state.status, state.pinned, state.updatedAt, state.lastMessageAt);
    }

    private BootstrapPayload.ConversationSummary toBootstrapConversation(ConversationState state) {
        return new BootstrapPayload.ConversationSummary(state.id, state.title, state.summary, "最近更新 " + state.updatedAt, state.mode, state.status, state.pinned);
    }

    private ConversationView toConversationView(ConversationState state) {
        return new ConversationView(state.id, state.projectId, state.title, state.summary, state.mode, state.status, state.pinned, state.updatedAt, state.lastMessageAt, state.context.conversationSummary, state.activeRunId);
    }

    private ContextPanelView contextFor(ConversationState state) {
        return new ContextPanelView(
                state.context.conversationSummary,
                state.context.decisions,
                state.context.openLoops,
                state.context.activeGoals,
                state.context.constraints,
                state.context.references,
                state.context.snapshots,
                state.context.updatedAt
        );
    }

    private void initializeContext(ConversationState state) {
        state.context.conversationSummary = "当前会话围绕 Phase 1 壳层、合同和联调基线推进。";
        state.context.decisions = List.of("交付最小真实接口", "保持文档与代码同步");
        state.context.openLoops = List.of("补齐 SSE 接线", "完成联调验证");
        state.context.activeGoals = List.of("推进真实主链路");
        state.context.constraints = List.of("保持会话优先", "保持 trace 可见");
        state.context.references = List.of(new ContextReferenceItem("ref-architecture", "架构设计", "file", "Phase 1 架构、后端模块设计与合同冻结文档"));
        state.context.snapshots = List.of(new ContextSnapshotView("snapshot-summary-" + state.id, project.id, state.id, "conversation_summary", state.summary, state.updatedAt));
        state.context.updatedAt = state.updatedAt;
    }

    private void updateContextAfterMessage(ConversationState state, String latestUserInput, String updatedAt) {
        state.context.updatedAt = updatedAt;
        state.context.conversationSummary = "会话已吸收最新需求，当前优先推进 Context 与 Settings/Capabilities 的真实数据链路。";
        state.context.decisions = List.of(
                "优先提交可验证的后端读接口",
                "保持前端 fallback 与远端双源能力",
                "先本地验证，再进入联调与生产机窗口"
        );
        state.context.openLoops = List.of(
                "Context 右侧面板消费真实接口",
                "Capabilities / Settings 页面消费真实读模型",
                "补全 go / no-go 结论"
        );
        state.context.activeGoals = List.of("推进真实主链路", abbreviate(latestUserInput));
        state.context.references = List.of(
                new ContextReferenceItem("ref-contract-freeze", "合同冻结", "file", "Phase 1 合同冻结、模块边界与错误口径"),
                new ContextReferenceItem("ref-latest-message", "最新消息", "conversation", abbreviate(latestUserInput))
        );
        state.context.snapshots = appendSnapshot(state.context.snapshots, new ContextSnapshotView(
                "snapshot-decision-" + UUID.randomUUID(),
                project.id,
                state.id,
                "decisions",
                String.join("；", state.context.decisions),
                updatedAt
        ));
    }

    private List<ContextSnapshotView> appendSnapshot(List<ContextSnapshotView> snapshots, ContextSnapshotView nextSnapshot) {
        List<ContextSnapshotView> next = new ArrayList<>(snapshots);
        next.add(0, nextSnapshot);
        return next.stream().limit(4).toList();
    }

    private String latestUserSummary(ConversationState state) {
        for (int index = state.messages.size() - 1; index >= 0; index--) {
            MessageView message = state.messages.get(index);
            if ("user".equals(message.role())) {
                return abbreviate(message.body());
            }
        }
        return "暂无用户输入";
    }

    private MessageView newMessage(String conversationId, String kind, String role, String body, String summary) {
        String at = now();
        return new MessageView("message-" + UUID.randomUUID(), project.id, conversationId, kind, role, body, summary, "assistant".equals(role) ? "已完成" : null, messageSeq.incrementAndGet(), at, at, List.of());
    }

    private MessageView latestMessageByKind(ConversationState state, String kind) {
        for (int index = state.messages.size() - 1; index >= 0; index--) {
            MessageView message = state.messages.get(index);
            if (kind.equals(message.kind())) {
                return message;
            }
        }
        return null;
    }

    private void appendMessageDeltaEvents(
            List<Map<String, Object>> events,
            String eventName,
            String projectId,
            String conversationId,
            String emittedAt,
            MessageView message
    ) {
        if (message == null || message.body() == null || message.body().isBlank()) {
            return;
        }

        List<String> chunks = chunkText(message.body());
        for (int index = 0; index < chunks.size(); index++) {
            events.add(event(eventName, projectId, conversationId, emittedAt, Map.of(
                    "messageId", message.id(),
                    "chunk", chunks.get(index),
                    "chunkIndex", index
            )));
        }
    }

    private List<String> chunkText(String value) {
        if (value.length() <= 24) {
            return List.of(value);
        }

        int middle = value.length() / 2;
        return List.of(value.substring(0, middle), value.substring(middle));
    }

    private Map<String, Object> event(String name, String projectId, String conversationId, String emittedAt, Map<String, Object> payload) {
        LinkedHashMap<String, Object> event = new LinkedHashMap<>();
        event.put("event", name);
        event.put("eventId", "evt-" + UUID.randomUUID());
        event.put("projectId", projectId);
        event.put("conversationId", conversationId);
        event.put("emittedAt", emittedAt);
        event.putAll(payload);
        return event;
    }

    private void requireProject(String projectId) {
        if (!project.id.equals(projectId)) throw new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project does not exist");
    }

    private ConversationState requireConversation(String conversationId) {
        ConversationState state = conversations.get(conversationId);
        if (state == null) throw new ApiException(HttpStatus.NOT_FOUND, "CONVERSATION_NOT_FOUND", "Conversation does not exist");
        return state;
    }

    private static String blankTo(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
    private static String abbreviate(String value) { return value.length() <= 48 ? value : value.substring(0, 47) + "…"; }
    private static String now() { return Instant.now().truncatedTo(ChronoUnit.SECONDS).toString(); }

    private static final class ProjectState {
        private final String id = "project-loom";
        private String name = "loom";
        private String description = "A projectized AI conversation workspace focused on visible trace, context, and capability controls.";
        private String status = "active";
        private String instructions = "Keep conversation-first UX, trace visibility, and document-first delivery discipline.";
        private final List<String> pinnedConversationIds = List.of("conversation-v1", "conversation-trace");
        private final CapabilityBindingSummary bindings = new CapabilityBindingSummary("model-gpt54-thinking", List.of("skill-planning", "skill-summarize", "skill-retrieve-context"), "routing-default");
        private String lastMessageAt = now();
        private String updatedAt = now();
    }

    private static final class ConversationState {
        private final String id;
        private final String projectId;
        private String title;
        private String summary = "等待第一条消息";
        private String mode;
        private String status = "idle";
        private boolean pinned;
        private String updatedAt = now();
        private String lastMessageAt = now();
        private String activeActionId;
        private String activeRunId;
        private final ContextState context = new ContextState();
        private final List<MessageView> messages = new ArrayList<>();

        private ConversationState(String id, String projectId, String title, String mode, boolean pinned) {
            this.id = id;
            this.projectId = projectId;
            this.title = title;
            this.mode = mode;
            this.pinned = pinned;
        }
    }

    private static final class ActionState {
        private final String id;
        private final String projectId;
        private final String conversationId;
        private final String runId;
        private final String title;
        private final String status;
        private final String summary;
        private final String startedAt;
        private final String completedAt;
        private final List<String> stepIds;

        private ActionState(
                String id,
                String projectId,
                String conversationId,
                String runId,
                String title,
                String status,
                String summary,
                String startedAt,
                String completedAt,
                List<String> stepIds
        ) {
            this.id = id;
            this.projectId = projectId;
            this.conversationId = conversationId;
            this.runId = runId;
            this.title = title;
            this.status = status;
            this.summary = summary;
            this.startedAt = startedAt;
            this.completedAt = completedAt;
            this.stepIds = List.copyOf(stepIds);
        }

        private ActionView toView() {
            return new ActionView(id, projectId, conversationId, runId, title, status, summary, startedAt, completedAt, stepIds);
        }
    }

    private static final class ContextState {
        private String conversationSummary = "当前会话围绕 Phase 1 壳层、合同和联调基线推进。";
        private List<String> decisions = List.of();
        private List<String> openLoops = List.of();
        private List<String> activeGoals = List.of();
        private List<String> constraints = List.of();
        private List<ContextReferenceItem> references = List.of();
        private List<ContextSnapshotView> snapshots = List.of();
        private String updatedAt = now();
        private int refreshCount = 0;
    }
}
