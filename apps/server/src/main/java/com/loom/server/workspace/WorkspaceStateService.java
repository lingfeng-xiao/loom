package com.loom.server.workspace;

import com.loom.server.api.ApiException;
import com.loom.server.model.BootstrapPayload;
import com.loom.server.workspace.WorkspaceDtos.CapabilityBindingSummary;
import com.loom.server.workspace.WorkspaceDtos.ContextPanelView;
import com.loom.server.workspace.WorkspaceDtos.ContextReferenceItem;
import com.loom.server.workspace.WorkspaceDtos.ContextRefreshResponse;
import com.loom.server.workspace.WorkspaceDtos.ContextSnapshotView;
import com.loom.server.workspace.WorkspaceDtos.ConversationListItem;
import com.loom.server.workspace.WorkspaceDtos.ConversationView;
import com.loom.server.workspace.WorkspaceDtos.CreateConversationRequest;
import com.loom.server.workspace.WorkspaceDtos.CursorPage;
import com.loom.server.workspace.WorkspaceDtos.McpServerView;
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
        state.messages.add(user);
        state.messages.add(thinking);
        state.messages.add(assistant);
        state.summary = abbreviate(request.body().trim());
        state.status = "active";
        state.updatedAt = assistant.completedAt();
        state.lastMessageAt = assistant.completedAt();
        state.activeRunId = "run-" + UUID.randomUUID();
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
        return new ContextRefreshResponse(getContext(projectId, conversationId));
    }

    public TracePanelView getTrace(String projectId, String conversationId) {
        requireProject(projectId);
        ConversationState state = requireConversation(conversationId);
        RunView run = state.activeRunId == null ? null : new RunView(state.activeRunId, "action-" + state.activeRunId, projectId, conversationId, "success", state.updatedAt, state.updatedAt, null);
        List<RunStepView> steps = List.of(
                new RunStepView("trace-context-" + conversationId, state.activeRunId, "读取上下文", "读取会话、设置与目标摘要", "success", state.updatedAt, state.updatedAt, null),
                new RunStepView("trace-reply-" + conversationId, state.activeRunId, "生成回复", "生成思考摘要与回复内容", state.activeRunId == null ? "running" : "success", state.updatedAt, state.activeRunId == null ? null : state.updatedAt, null),
                new RunStepView("trace-followup-" + conversationId, state.activeRunId, "准备后续联调", "等待下一次主链路联调或刷新", state.activeRunId == null ? "pending" : "success", state.updatedAt, state.activeRunId == null ? null : state.updatedAt, null)
        );
        return new TracePanelView("当前会先读取前端文档输入，再把 Phase 1 收敛到可接线状态。", run, steps, state.updatedAt);
    }

    public RunView getRun(String projectId, String conversationId, String runId) {
        TracePanelView trace = getTrace(projectId, conversationId);
        if (trace.activeRun() == null || !trace.activeRun().id().equals(runId)) throw new ApiException(HttpStatus.NOT_FOUND, "RUN_NOT_FOUND", "Run does not exist");
        return trace.activeRun();
    }

    public SettingsOverviewView getSettingsOverview(String scope) {
        if (scope == null || scope.isBlank() || scope.equals(settings.activeScope())) return settings;
        return new SettingsOverviewView(scope, settings.tabs(), settings.modelProfiles(), settings.skills(), settings.mcpServers(), settings.memoryPolicy(), settings.routingPolicy());
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
        conversations.put(id, state);
    }

    private ConversationListItem toConversationListItem(ConversationState state) {
        return new ConversationListItem(state.id, state.projectId, state.title, state.summary, state.mode, state.status, state.pinned, state.updatedAt, state.lastMessageAt);
    }

    private BootstrapPayload.ConversationSummary toBootstrapConversation(ConversationState state) {
        return new BootstrapPayload.ConversationSummary(state.id, state.title, state.summary, "最近更新 " + state.updatedAt, state.mode, state.status, state.pinned);
    }

    private ConversationView toConversationView(ConversationState state) {
        return new ConversationView(state.id, state.projectId, state.title, state.summary, state.mode, state.status, state.pinned, state.updatedAt, state.lastMessageAt, "当前会话围绕 Phase 1 壳层、合同和联调基线推进。", state.activeRunId);
    }

    private ContextPanelView contextFor(ConversationState state) {
        return new ContextPanelView(
                "当前会话围绕 Phase 1 壳层、合同和联调基线推进。",
                List.of("交付最小真实接口", "保持文档与代码同步"),
                List.of("补齐 SSE 接线", "完成联调验证"),
                List.of("推进真实主链路"),
                List.of("保持会话优先", "保持 trace 可见"),
                List.of(new ContextReferenceItem("ref-architecture", "架构设计", "file", "Phase 1 架构、后端模块设计与合同冻结文档")),
                List.of(new ContextSnapshotView("snapshot-summary-" + state.id, project.id, state.id, "conversation_summary", state.summary, state.updatedAt)),
                state.updatedAt
        );
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
        private String activeRunId;
        private final List<MessageView> messages = new ArrayList<>();

        private ConversationState(String id, String projectId, String title, String mode, boolean pinned) {
            this.id = id;
            this.projectId = projectId;
            this.title = title;
            this.mode = mode;
            this.pinned = pinned;
        }
    }
}
