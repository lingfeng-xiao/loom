package com.loom.server.workspace;

import com.loom.server.api.ApiException;
import com.loom.server.config.LoomLlmProperties;
import com.loom.server.context.ConversationContextInputs;
import com.loom.server.context.ConversationContextService;
import com.loom.server.memory.MemoryItemCommand;
import com.loom.server.memory.MemoryQuery;
import com.loom.server.memory.MemoryService;
import com.loom.server.memory.MemorySuggestionCommand;
import com.loom.server.memory.MemorySuggestionView;
import com.loom.server.model.BootstrapPayload;
import com.loom.server.workspace.MinimaxChatClient.ChatCompletionRequest;
import com.loom.server.workspace.MinimaxChatClient.ChatCompletionResult;
import com.loom.server.workspace.MinimaxChatClient.ChatMessage;
import com.loom.server.workspace.WorkspaceDtos.ActionView;
import com.loom.server.workspace.WorkspaceDtos.CapabilityBindingRuleView;
import com.loom.server.workspace.WorkspaceDtos.CapabilityBindingSummary;
import com.loom.server.workspace.WorkspaceDtos.CapabilityCardView;
import com.loom.server.workspace.WorkspaceDtos.CapabilityOverviewView;
import com.loom.server.workspace.WorkspaceDtos.ContextPanelView;
import com.loom.server.workspace.WorkspaceDtos.ContextReferenceItem;
import com.loom.server.workspace.WorkspaceDtos.ContextRefreshResponse;
import com.loom.server.workspace.WorkspaceDtos.ContextSnapshotView;
import com.loom.server.workspace.WorkspaceDtos.ConversationListItem;
import com.loom.server.workspace.WorkspaceDtos.ConversationView;
import com.loom.server.workspace.WorkspaceDtos.CreateConversationRequest;
import com.loom.server.workspace.WorkspaceDtos.CreateProjectRequest;
import com.loom.server.workspace.WorkspaceDtos.CursorPage;
import com.loom.server.workspace.WorkspaceDtos.FileAssetSummaryView;
import com.loom.server.workspace.WorkspaceDtos.LlmConnectionTestView;
import com.loom.server.workspace.WorkspaceDtos.LlmModelOptionView;
import com.loom.server.workspace.WorkspaceDtos.LlmProviderPresetView;
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
import com.loom.server.workspace.WorkspaceDtos.UpdateLlmConfigRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Service
public class WorkspaceStateService {
    private static final List<String> SETTINGS_TABS = List.of("Models", "Skills", "MCP", "Memory", "Routing");

    private final AtomicInteger projectSeq = new AtomicInteger(1);
    private final AtomicInteger conversationSeq = new AtomicInteger(5);
    private final AtomicInteger messageSeq = new AtomicInteger(10);
    private final LinkedHashMap<String, ProjectState> projects = new LinkedHashMap<>();
    private final LinkedHashMap<String, ConversationState> conversations = new LinkedHashMap<>();
    private final LinkedHashMap<String, ActionState> actions = new LinkedHashMap<>();
    private final LinkedHashMap<String, List<FileAssetSummaryView>> fileAssetsByProject = new LinkedHashMap<>();
    private final List<MemoryItemView> memoryItems = new ArrayList<>();
    private final List<SkillView> skills = List.of(
            new SkillView("skill-planning", "project", "planning", true, "internal"),
            new SkillView("skill-summarize", "project", "summarize", true, "internal"),
            new SkillView("skill-retrieve-context", "project", "retrieve-context", true, "internal")
    );
    private final List<McpServerView> mcpServers = List.of(
            new McpServerView("mcp-local-dev", "project", "local-dev", "unconfigured", 0, 0, 0),
            new McpServerView("mcp-notion", "project", "notion-mcp", "unconfigured", 0, 0, 0)
    );
    private final MemoryPolicyView memoryPolicy = new MemoryPolicyView("memory-default", "project", true, true, true);
    private final RoutingPolicyView routingPolicy = new RoutingPolicyView("routing-default", "project", "internal", false, "not configured");
    private final List<LlmProviderPresetView> providerPresets = providerPresets();
    private final MinimaxChatClient minimaxChatClient;
    private final LlmSettingsRepository llmSettingsRepository;
    private final ConversationContextService conversationContextService;
    private final MemoryService memoryService;
    private final LinkedHashMap<String, LlmConfigState> llmConfigs = new LinkedHashMap<>();
    private LlmConnectionTestView lastConnectionTest;

    public WorkspaceStateService(
            LoomLlmProperties llmProperties,
            MinimaxChatClient minimaxChatClient,
            LlmSettingsRepository llmSettingsRepository,
            ConversationContextService conversationContextService,
            MemoryService memoryService
    ) {
        this.minimaxChatClient = minimaxChatClient;
        this.llmSettingsRepository = llmSettingsRepository;
        this.conversationContextService = conversationContextService;
        this.memoryService = memoryService;
        LoomLlmProperties.Minimax minimax = llmProperties.getMinimax();
        String now = now();
        LlmConfigState defaultConfig = new LlmConfigState(
                "profile-minimax-cn",
                "minimax-cn",
                "MiniMax",
                blankTo(minimax.getDisplayName(), "MiniMax M2.7"),
                blankTo(minimax.getApiBaseUrl(), "https://api.minimaxi.com/v1"),
                blankTo(minimax.getModelId(), "MiniMax-M2.7"),
                minimax.getApiKey() == null ? "" : minimax.getApiKey().trim(),
                blankTo(minimax.getSystemPrompt(), "You are Loom, a careful AI teammate that explains decisions clearly and keeps execution visible."),
                minimax.getTemperature(),
                minimax.getMaxTokens(),
                minimax.getTimeoutMs(),
                true,
                now
        );
        List<LlmConfigState> loadedConfigs = llmSettingsRepository.loadProfiles();
        if (loadedConfigs.isEmpty()) {
            llmConfigs.put(defaultConfig.id, defaultConfig);
        } else {
            loadedConfigs.forEach(config -> llmConfigs.put(config.id, config));
            ensureSingleActiveConfig();
        }
        seedWorkspace();
    }

    public CursorPage<ProjectListItem> listProjects() {
        return new CursorPage<>(projects.values().stream().sorted(Comparator.comparing((ProjectState p) -> p.updatedAt).reversed()).map(this::toProjectListItem).toList(), null, false);
    }

    public ProjectView createProject(CreateProjectRequest request) {
        String projectName = request == null || request.name() == null || request.name().isBlank()
                ? nextGeneratedProjectName()
                : request.name().trim();

        String id = nextProjectId(projectName);
        ProjectState state = newProject(
                id,
                projectName,
                blankTo(request == null ? null : request.description(), "New project ready for Loom sessions."),
                blankTo(request == null ? null : request.instructions(), "Start a new session here and keep the trace visible.")
        );
        projects.put(id, state);
        fileAssetsByProject.put(id, List.of());
        memoryItems.add(new MemoryItemView("memory-project-" + id, "project", id, null, "Project created from the new session flow.", "explicit", now()));
        return toProjectView(state);
    }

    public ProjectView getProject(String projectId) {
        return toProjectView(requireProject(projectId));
    }

    public CursorPage<ConversationListItem> listConversations(String projectId) {
        requireProject(projectId);
        return new CursorPage<>(projectConversations(projectId).stream().sorted(Comparator.comparing((ConversationState c) -> c.updatedAt).reversed()).map(this::toConversationListItem).toList(), null, false);
    }

    public ConversationView createConversation(String projectId, CreateConversationRequest request) {
        ProjectState project = requireProject(projectId);
        String createdAt = now();
        ConversationState state = new ConversationState(
                "conversation-" + conversationSeq.incrementAndGet(),
                projectId,
                blankTo(request == null ? null : request.title(), "New chat"),
                blankTo(request == null ? null : request.mode(), "chat"),
                false,
                createdAt
        );
        initializeContext(state);
        conversations.put(state.id, state);
        project.updatedAt = createdAt;
        return toConversationView(state);
    }

    public ConversationView getConversation(String projectId, String conversationId) {
        return toConversationView(requireConversation(projectId, conversationId));
    }

    public ConversationView updateConversation(String projectId, String conversationId, UpdateConversationRequest request) {
        ConversationState state = requireConversation(projectId, conversationId);
        if (request != null) {
            if (request.projectId() != null && !request.projectId().isBlank() && !request.projectId().equals(state.projectId)) {
                moveConversationToProject(state, requireProject(request.projectId().trim()));
            }
            state.title = blankTo(request.title(), state.title);
            state.mode = blankTo(request.mode(), state.mode);
            state.status = blankTo(request.status(), state.status);
            state.pinned = request.pinned() != null ? request.pinned() : state.pinned;
            state.updatedAt = now();
        }
        return toConversationView(state);
    }

    public CursorPage<MessageView> listMessages(String projectId, String conversationId) {
        return new CursorPage<>(List.copyOf(requireConversation(projectId, conversationId).messages), null, false);
    }

    public synchronized SubmitMessageResponse submitMessage(String projectId, String conversationId, SubmitMessageRequest request) {
        ProjectState project = requireProject(projectId);
        ConversationState state = requireConversation(projectId, conversationId);
        if (request == null || request.body() == null || request.body().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MESSAGE_BODY_REQUIRED", "Message body is required");
        }
        if (state.pendingTurn != null) {
            throw new ApiException(HttpStatus.CONFLICT, "CONVERSATION_BUSY", "Wait for the active streamed response to finish before sending another message");
        }

        String startedAt = now();
        String userBody = request.body().trim();
        MessageView user = newMessage(projectId, conversationId, "user", "user", userBody, null, null, startedAt, startedAt, null);
        ActionState action = newAction(projectId, conversationId, summarize(userBody), "running", startedAt, null);
        state.pendingTurn = new PendingAssistantTurn(
                projectId,
                conversationId,
                action.id,
                action.runId,
                userBody,
                request.allowMemory() == null || request.allowMemory(),
                startedAt,
                "message-" + UUID.randomUUID(),
                "message-" + UUID.randomUUID()
        );

        state.messages.add(user);
        state.summary = summarize(userBody);
        state.status = "active";
        state.updatedAt = startedAt;
        state.lastMessageAt = startedAt;
        state.activeActionId = action.id;
        state.activeRunId = action.runId;
        state.traceSummary = "Preparing the next streamed reply.";
        state.traceUpdatedAt = startedAt;
        state.traceSteps.clear();
        state.traceSteps.addAll(defaultRunningTraceSteps(conversationId, action.runId, startedAt));
        state.context.updatedAt = startedAt;
        state.context.conversationSummary = "The latest request was accepted and the reply is now streaming.";
        state.context.activeGoals = List.of("Answer the active request.", summarize(userBody));
        state.context.references = List.of(new ContextReferenceItem("ref-latest-message", "Latest message", "conversation", summarize(userBody)));
        project.updatedAt = state.updatedAt;
        project.lastMessageAt = state.lastMessageAt;
        return new SubmitMessageResponse(conversationId, user, state.activeRunId, "/api/projects/" + projectId + "/conversations/" + conversationId + "/stream");
    }

    public ContextPanelView getContext(String projectId, String conversationId) {
        return assembleContextPanel(requireConversation(projectId, conversationId));
    }

    public CursorPage<ContextSnapshotView> listContextSnapshots(String projectId, String conversationId, String cursor, Integer limit) {
        ConversationState state = requireConversation(projectId, conversationId);
        return page(mergedContextSnapshots(state), cursor, limit);
    }

    public ContextRefreshResponse refreshContext(String projectId, String conversationId) {
        ConversationState state = requireConversation(projectId, conversationId);
        String refreshedAt = now();
        state.context.refreshCount++;
        state.context.updatedAt = refreshedAt;
        state.context.conversationSummary = "The context panel was refreshed from the latest session state.";
        state.context.decisions = List.of("Use the configured model when available.", "Keep new sessions project-first.", "Record acceptance evidence after changes.");
        state.context.openLoops = List.of("Retest model connectivity.", "Validate project and session creation.", "Confirm stream rendering.");
        state.context.activeGoals = List.of("Keep the real-model path stable.", "Refresh #" + state.context.refreshCount);
        state.context.references = List.of(
                new ContextReferenceItem("ref-settings", "LLM settings", "memory", "MiniMax preset, endpoint, model, and API key."),
                new ContextReferenceItem("ref-message", "Latest user request", "conversation", latestUserSummary(state))
        );
        ContextSnapshotView snapshot = conversationContextService.storeSnapshot(contextInputsFor(state, "active_context", refreshedAt));
        state.context.snapshots = mergeSnapshots(state.context.snapshots, List.of(snapshot));
        state.updatedAt = refreshedAt;
        return new ContextRefreshResponse(assembleContextPanel(state));
    }

    public TracePanelView getTrace(String projectId, String conversationId) {
        ConversationState state = requireConversation(projectId, conversationId);
        LlmConfigState activeConfig = activeLlmConfig();
        ActionView action = state.activeActionId == null ? null : actionView(state.activeActionId);
        RunView run = action == null
                ? null
                : new RunView(
                action.runId(),
                action.id(),
                projectId,
                conversationId,
                toRunStatus(action.status()),
                action.startedAt(),
                action.completedAt(),
                null
        );
        List<RunStepView> steps = state.traceSteps.isEmpty()
                ? defaultPreviewTraceSteps(conversationId, state.activeRunId, state.updatedAt)
                : List.copyOf(state.traceSteps);
        return new TracePanelView(
                blankTo(state.traceSummary, activeConfig.isConfigured() ? "The active chat path is using the configured " + activeConfig.provider + " endpoint." : "No live model is configured yet. Settings can finish provider setup."),
                action,
                run,
                steps,
                blankTo(state.traceUpdatedAt, state.updatedAt)
        );
    }

    public RunView getRun(String projectId, String conversationId, String runId) {
        TracePanelView trace = getTrace(projectId, conversationId);
        if (trace.activeRun() == null || !trace.activeRun().id().equals(runId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "RUN_NOT_FOUND", "Run does not exist");
        }
        return trace.activeRun();
    }

    public ActionView getAction(String projectId, String conversationId, String actionId) {
        requireConversation(projectId, conversationId);
        ActionView action = actionView(actionId);
        if (!action.projectId().equals(projectId) || !action.conversationId().equals(conversationId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ACTION_NOT_FOUND", "Action does not exist");
        }
        return action;
    }

    public CursorPage<RunStepView> listRunSteps(String projectId, String conversationId, String runId) {
        TracePanelView trace = getTrace(projectId, conversationId);
        if (trace.activeRun() == null || !trace.activeRun().id().equals(runId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "RUN_NOT_FOUND", "Run does not exist");
        }
        return new CursorPage<>(trace.steps(), null, false);
    }

    public CursorPage<FileAssetSummaryView> listFiles(String projectId) {
        requireProject(projectId);
        return new CursorPage<>(fileAssetsByProject.getOrDefault(projectId, List.of()), null, false);
    }

    public CursorPage<MemoryItemView> listMemory(String projectId, String conversationId, String scope, String cursor, Integer limit) {
        requireProject(projectId);
        CursorPage<MemoryItemView> persisted = memoryService.listMemory(new MemoryQuery(projectId, conversationId, scope, cursor, limit));
        if (!persisted.items().isEmpty()) {
            return persisted;
        }
        List<MemoryItemView> fallbackItems = memoryItems.stream()
                .filter(item -> item.projectId() == null || Objects.equals(item.projectId(), projectId))
                .filter(item -> matchesMemoryScope(scope, conversationId, item))
                .sorted(Comparator.comparing(MemoryItemView::updatedAt).reversed())
                .toList();
        return page(fallbackItems, cursor, limit);
    }

    public MemoryItemView createMemoryItem(String projectId, MemoryItemCommand command) {
        requireProject(projectId);
        return memoryService.createMemoryItem(new MemoryItemCommand(projectId, command.conversationId(), command.scope(), command.content(), command.source()));
    }

    public MemoryItemView updateMemoryItem(String projectId, String memoryId, MemoryItemCommand command) {
        requireProject(projectId);
        return memoryService.updateMemoryItem(projectId, memoryId, new MemoryItemCommand(projectId, command.conversationId(), command.scope(), command.content(), command.source()));
    }

    public void deleteMemoryItem(String projectId, String memoryId) {
        requireProject(projectId);
        memoryService.deleteMemoryItem(projectId, memoryId);
    }

    public CursorPage<MemorySuggestionView> listMemorySuggestions(String projectId, String conversationId, String scope, String cursor, Integer limit) {
        requireProject(projectId);
        return memoryService.listSuggestions(new MemoryQuery(projectId, conversationId, scope, cursor, limit));
    }

    public MemorySuggestionView createMemorySuggestion(String projectId, MemorySuggestionCommand command) {
        requireProject(projectId);
        return memoryService.suggestMemory(new MemorySuggestionCommand(projectId, command.conversationId(), command.scope(), command.content()));
    }

    public MemorySuggestionView acceptMemorySuggestion(String projectId, String suggestionId) {
        requireProject(projectId);
        return memoryService.acceptSuggestion(projectId, suggestionId);
    }

    public MemorySuggestionView rejectMemorySuggestion(String projectId, String suggestionId) {
        requireProject(projectId);
        return memoryService.rejectSuggestion(projectId, suggestionId);
    }

    public SettingsOverviewView getSettingsOverview(String scope) {
        String resolvedScope = blankTo(scope, "project");
        List<LlmConfigState> orderedConfigs = orderedLlmConfigs();
        LlmConfigState activeConfig = activeLlmConfig();
        return new SettingsOverviewView(
                resolvedScope,
                SETTINGS_TABS,
                orderedConfigs.stream()
                        .map(config -> new ModelProfileView(
                                "model-profile-" + config.id,
                                config.presetId,
                                resolvedScope,
                                config.isConfigured() ? config.displayName : config.displayName + " (setup required)",
                                config.provider,
                                config.modelId,
                                config.isConfigured(),
                                config.active,
                                true,
                                false,
                                true,
                                true,
                                true,
                                config.timeoutMs
                        ))
                        .toList(),
                skills,
                mcpServers,
                memoryPolicy,
                routingPolicy,
                providerPresets,
                orderedConfigs.stream().map(LlmConfigState::toView).toList(),
                activeConfig.toView(),
                lastConnectionTest
        );
    }

    public SettingsOverviewView updateLlmConfiguration(UpdateLlmConfigRequest request) {
        LlmConfigState config = upsertLlmConfig(request);
        config.updatedAt = now();
        boolean shouldActivate = config.active
                || Boolean.TRUE.equals(request == null ? null : request.activate())
                || (request != null && request.apiKey() != null && !request.apiKey().isBlank());
        if (shouldActivate) {
            activateConfig(config.id);
        }
        persistLlmConfigs();
        return getSettingsOverview("project");
    }

    public LlmConnectionTestView testLlmConfiguration(UpdateLlmConfigRequest request) {
        LlmConfigState baseConfig = resolveBaseConfig(request);
        LlmConfigState candidate = baseConfig.copy();
        candidate.apply(request, providerPresets);
        if (!candidate.isConfigured()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "LLM_API_KEY_REQUIRED", candidate.provider + " API key is required before testing the connection");
        }
        ChatCompletionResult result = minimaxChatClient.complete(new ChatCompletionRequest(
                candidate.apiBaseUrl,
                candidate.apiKey,
                candidate.modelId,
                candidate.temperature,
                candidate.maxTokens,
                candidate.timeoutMs,
                List.of(
                        new ChatMessage("system", candidate.systemPrompt),
                        new ChatMessage("user", "Reply with a short confirmation that the Loom MiniMax connection works.")
                )
        ));
        lastConnectionTest = new LlmConnectionTestView(true, candidate.provider, result.responseModel(), candidate.apiBaseUrl, "Connection test succeeded.", summarize(result.outputText()), now(), result.latencyMs());
        return lastConnectionTest;
    }

    public CapabilityOverviewView getCapabilitiesOverview(String scope) {
        SettingsOverviewView settings = getSettingsOverview(scope);
        return new CapabilityOverviewView(
                settings.activeScope(),
                "Capabilities now include a real LLM configuration path plus skill, MCP, and routing summaries.",
                List.of(
                        new CapabilityCardView("cap-models", "Models", "Current project model binding.", settings.modelProfiles().stream().map(ModelProfileView::name).toList()),
                        new CapabilityCardView("cap-skills", "Skills", "Enabled internal skills.", settings.skills().stream().map(SkillView::name).toList()),
                        new CapabilityCardView("cap-mcp", "MCP Servers", "Configured MCP connection status.", settings.mcpServers().stream().map(McpServerView::name).toList()),
                        new CapabilityCardView("cap-executors", "Executors", "Runtime and external executor routing.", List.of(settings.routingPolicy().defaultRuntime(), settings.routingPolicy().allowExternalExecutors() ? settings.routingPolicy().externalExecutorLabel() : "not configured"))
                ),
                List.of(
                        new CapabilityBindingRuleView("Default chat model", settings.modelProfiles().get(0).name(), "accent"),
                        new CapabilityBindingRuleView("Enabled skills", String.valueOf(settings.skills().size()), "good"),
                        new CapabilityBindingRuleView("Routing policy", settings.routingPolicy().id(), "neutral")
                )
        );
    }

    public void streamConversation(String projectId, String conversationId, Consumer<Map<String, Object>> sink) {
        ConversationState state = requireConversation(projectId, conversationId);
        PendingAssistantTurn pendingTurn = state.pendingTurn;
        if (pendingTurn == null) {
            replayLatestConversationEvents(projectId, conversationId, sink);
            return;
        }
        if (pendingTurn.streamingStarted) {
            replayLatestConversationEvents(projectId, conversationId, sink);
            return;
        }
        pendingTurn.streamingStarted = true;

        ProjectState project = requireProject(projectId);
        ActionState action = actions.get(pendingTurn.actionId);
        if (action == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ACTION_NOT_FOUND", "Action does not exist");
        }

        String runId = pendingTurn.runId;
        emitCurrentTraceSteps(projectId, conversationId, runId, sink);

        MessageAccumulator thinking = new MessageAccumulator("thinking_summary", pendingTurn.thinkingMessageId, pendingTurn.startedAt);
        MessageAccumulator assistant = new MessageAccumulator("assistant", pendingTurn.assistantMessageId, pendingTurn.startedAt);
        long startedAtMs = Instant.parse(pendingTurn.startedAt).toEpochMilli();

        try {
            completeContextStep(projectId, conversationId, runId, pendingTurn.startedAt, sink);
            markTraceStep(projectId, conversationId, runId, "trace-model-" + conversationId, "running", "Calling the configured model and streaming back deltas.", null, sink);
            updateTraceSummary(state, "Reasoning through the request and composing a streamed reply.", pendingTurn.startedAt);

            StreamedAssistantTurn streamedTurn = generateAssistantTurn(project, state, pendingTurn, sink, thinking, assistant);
            String completedAt = now();
            long latencyMs = Math.max(1L, Instant.parse(completedAt).toEpochMilli() - startedAtMs);
            MessageView thinkingMessage = finalizeThinkingMessage(projectId, conversationId, thinking, streamedTurn, completedAt, latencyMs);
            MessageView assistantMessage = finalizeAssistantMessage(projectId, conversationId, assistant, streamedTurn, completedAt, latencyMs);

            if (thinkingMessage != null) {
                state.messages.add(thinkingMessage);
                sink.accept(event("thinking.summary.done", projectId, conversationId, completedAt, Map.of("message", thinkingMessage)));
            }
            state.messages.add(assistantMessage);
            sink.accept(event("message.done", projectId, conversationId, completedAt, Map.of("message", assistantMessage)));

            markTraceStep(projectId, conversationId, runId, "trace-model-" + conversationId, "success", "The model completed streaming successfully.", completedAt, sink);
            markTraceStep(projectId, conversationId, runId, "trace-publish-" + conversationId, "running", "Persisting messages, trace, and refreshed context.", null, sink);

            state.summary = summarize(pendingTurn.userInput);
            state.updatedAt = completedAt;
            state.lastMessageAt = completedAt;
            state.traceSummary = blankTo(streamedTurn.reasoningSummary(), "Reply streamed successfully.");
            state.traceUpdatedAt = completedAt;
            updateContextAfterMessage(projectId, state, pendingTurn.userInput, assistantMessage.body(), completedAt);
            ContextPanelView context = persistContextSnapshot(state, "turn", completedAt);

            action.status = "completed";
            action.summary = summarize(assistantMessage.body());
            action.completedAt = completedAt;
            markTraceStep(projectId, conversationId, runId, "trace-publish-" + conversationId, "success", "Conversation state and context are updated.", completedAt, sink);

            TracePanelView trace = getTrace(projectId, conversationId);
            sink.accept(event("context.updated", projectId, conversationId, completedAt, Map.of("context", context)));
            if (pendingTurn.allowMemory) {
                MemorySuggestionView suggestion = createMemorySuggestion(projectId, conversationId, pendingTurn.userInput, assistantMessage.body());
                if (suggestion != null) {
                    sink.accept(event("memory.suggested", projectId, conversationId, completedAt, Map.of("suggestion", suggestion)));
                }
            }
            sink.accept(event("run.completed", projectId, conversationId, completedAt, Map.of("run", trace.activeRun())));

            project.updatedAt = completedAt;
            project.lastMessageAt = completedAt;
            state.pendingTurn = null;
        } catch (Exception exception) {
            String failedAt = now();
            MessageView failureMessage = newMessage(
                    projectId,
                    conversationId,
                    "assistant",
                    "assistant",
                    "The streamed reply failed before completion. Check the model connection and try again.\n\nError: " + blankTo(exception.getMessage(), "Unknown model error"),
                    "Streaming failed",
                    "failed",
                    pendingTurn.startedAt,
                    failedAt,
                    Math.max(1L, Instant.parse(failedAt).toEpochMilli() - startedAtMs)
            );
            state.messages.add(failureMessage);
            state.updatedAt = failedAt;
            state.lastMessageAt = failedAt;
            state.traceSummary = "The streamed run failed before the reply could finish.";
            state.traceUpdatedAt = failedAt;
            action.status = "failed";
            action.summary = blankTo(exception.getMessage(), "Model request failed");
            action.completedAt = failedAt;
            project.updatedAt = failedAt;
            project.lastMessageAt = failedAt;
            markTraceStep(projectId, conversationId, runId, "trace-model-" + conversationId, "failed", blankTo(exception.getMessage(), "Model request failed"), failedAt, sink);
            markTraceStep(projectId, conversationId, runId, "trace-publish-" + conversationId, "skipped", "Publishing the final reply was skipped because the run failed.", failedAt, sink);
            sink.accept(event("message.done", projectId, conversationId, failedAt, Map.of("message", failureMessage)));
            sink.accept(event("run.failed", projectId, conversationId, failedAt, Map.of(
                    "run", new RunView(runId, action.id, projectId, conversationId, "failed", action.startedAt, failedAt, null),
                    "error", Map.of("code", "LLM_REQUEST_FAILED", "message", blankTo(exception.getMessage(), "Model request failed"))
            )));
            state.pendingTurn = null;
        }
    }

    public BootstrapPayload buildBootstrapPayload() {
        ConversationState active = conversations.values().stream().sorted(Comparator.comparing((ConversationState c) -> c.updatedAt).reversed()).findFirst().orElseThrow();
        ProjectState project = requireProject(active.projectId);
        ContextPanelView context = assembleContextPanel(active);
        TracePanelView trace = getTrace(project.id, active.id);
        SettingsOverviewView settings = getSettingsOverview("project");

        return new BootstrapPayload(
                "loom",
                "Projectized AI workspace with visible trace, context, and model configuration.",
                new BootstrapPayload.ProjectSummary(project.id, project.name, "Live workspace", project.description, "Project: " + project.name, "Updated " + project.updatedAt, activeLlmConfig().isConfigured() ? "model-ready" : "model-missing"),
                List.of(
                        new BootstrapPayload.WorkspacePageLink("conversation", "Conversation", "Project sessions", "Cmd+1", true),
                        new BootstrapPayload.WorkspacePageLink("capabilities", "Capabilities", "Models, MCP, skills, and routing", "Cmd+2", true),
                        new BootstrapPayload.WorkspacePageLink("openclaw", "Automation", "External executor visibility", "Cmd+3", true),
                        new BootstrapPayload.WorkspacePageLink("files", "Files", "Project files", "Cmd+4", true),
                        new BootstrapPayload.WorkspacePageLink("memory", "Memory", "Project and conversation memory", "Cmd+5", true),
                        new BootstrapPayload.WorkspacePageLink("settings", "Settings", "Model configuration and policy", "Cmd+6", true)
                ),
                projectConversations(project.id).stream().filter(c -> !c.pinned).sorted(Comparator.comparing((ConversationState c) -> c.updatedAt).reversed()).map(this::toBootstrapConversation).toList(),
                projectConversations(project.id).stream().filter(c -> c.pinned).sorted(Comparator.comparing((ConversationState c) -> c.updatedAt).reversed()).map(this::toBootstrapConversation).toList(),
                List.of(
                        new BootstrapPayload.WorkspaceModeOption("chat", "Chat", "Direct collaboration with the active project."),
                        new BootstrapPayload.WorkspaceModeOption("plan", "Plan", "Focus on scope, tradeoffs, and execution steps."),
                        new BootstrapPayload.WorkspaceModeOption("action", "Action", "Highlight tool execution and follow-through."),
                        new BootstrapPayload.WorkspaceModeOption("review", "Review", "Inspect outcomes, gaps, and acceptance.")
                ),
                active.mode,
                active.title,
                project.name + " | " + active.summary,
                active.messages.stream().map(message -> new BootstrapPayload.ConversationMessage(message.id(), message.kind(), bootstrapMessageLabel(message), message.body(), message.summary(), message.statusLabel())).toList(),
                new BootstrapPayload.ComposerState("Describe the next step for the active Loom project.", "Send", List.of("Attach context", "Attach file", "Open command palette"), List.of(new BootstrapPayload.ComposerToggle("Allow actions", true), new BootstrapPayload.ComposerToggle("Memory", true), new BootstrapPayload.ComposerToggle("Upload", false))),
                trace.reasoningSummary(),
                trace.steps().stream().map(step -> new BootstrapPayload.TraceStep(step.id(), step.title(), step.detail(), step.status())).toList(),
                List.of(
                        new BootstrapPayload.ContextBlock("context-goal", "Current goal", context.activeGoals().isEmpty() ? "Start the next session step." : context.activeGoals().get(0)),
                        new BootstrapPayload.ContextBlock("context-constraints", "Constraints", String.join(" | ", context.constraints())),
                        new BootstrapPayload.ContextBlock("context-summary", "Summary", context.conversationSummary()),
                        new BootstrapPayload.ContextBlock("context-active", "Active work", String.join(" | ", context.decisions())),
                        new BootstrapPayload.ContextBlock("context-files", "Reference", context.references().stream().map(ContextReferenceItem::summary).findFirst().orElse("No linked references yet.")),
                        new BootstrapPayload.ContextBlock("context-open", "Open loops", String.join(" | ", context.openLoops()))
                ),
                new BootstrapPayload.CapabilitiesOverview(
                        "The current workspace shows the active model binding, skill stack, MCP connections, and executor routing in one place.",
                        List.of(
                                new BootstrapPayload.OverviewCard("cap-models", "Models", "Configured model profile", settings.modelProfiles().stream().map(ModelProfileView::name).toList()),
                                new BootstrapPayload.OverviewCard("cap-mcp", "MCP Servers", "Configured servers", settings.mcpServers().stream().map(McpServerView::name).toList()),
                                new BootstrapPayload.OverviewCard("cap-skills", "Skills", "Enabled skills", settings.skills().stream().map(SkillView::name).toList()),
                                new BootstrapPayload.OverviewCard("cap-executors", "Executors", "Current runtime routing", List.of("internal", "external executor not configured"))
                        ),
                        List.of(
                                new BootstrapPayload.StatusItem("Default chat model", settings.modelProfiles().get(0).name(), "accent"),
                                new BootstrapPayload.StatusItem("External tasks", "not configured", "warn"),
                                new BootstrapPayload.StatusItem("Session creation", "Project-first", "neutral")
                        )
                ),
                new BootstrapPayload.OpenClawOverview(
                        "External executor integration is visible here once configured.",
                        List.of(new BootstrapPayload.DetailItem("Gateway", "not configured"), new BootstrapPayload.DetailItem("Status", "not connected"), new BootstrapPayload.DetailItem("Last heartbeat", "not available")),
                        List.of(new BootstrapPayload.StatusItem("Channels", "0", "neutral"), new BootstrapPayload.StatusItem("Tools", "0", "neutral"), new BootstrapPayload.StatusItem("Skills", "0", "neutral"), new BootstrapPayload.StatusItem("Plugins", "0", "neutral")),
                        List.of(new BootstrapPayload.StatusItem("Async work", "not configured", "warn"), new BootstrapPayload.StatusItem("Chat replies", activeLlmConfig().isConfigured() ? activeLlmConfig().provider : "Setup required", activeLlmConfig().isConfigured() ? "good" : "warn"), new BootstrapPayload.StatusItem("External executor", "pending setup", "neutral")),
                        trace.steps().stream().limit(3).map(step -> new BootstrapPayload.StatusItem(step.id(), step.status(), "success".equals(step.status()) ? "good" : "warn")).toList(),
                        projectConversations(project.id).stream().filter(c -> c.pinned).map(c -> c.title).toList()
                ),
                new BootstrapPayload.SettingsOverview(
                        "Configure model bindings, project policy, and routing from one settings surface.",
                        SETTINGS_TABS,
                        List.of(
                                new BootstrapPayload.DetailItem("Profile", settings.modelProfiles().get(0).name()),
                                new BootstrapPayload.DetailItem("Provider", settings.activeLlmConfig().provider()),
                                new BootstrapPayload.DetailItem("Model", settings.activeLlmConfig().modelId()),
                                new BootstrapPayload.DetailItem("Endpoint", settings.activeLlmConfig().apiBaseUrl()),
                                new BootstrapPayload.DetailItem("Timeout", settings.activeLlmConfig().timeoutMs() + " ms")
                        ),
                        List.of("New sessions now start from project selection or project creation.", "MiniMax and Kimi presets are live; other providers remain unconfigured.", "Run a connection test after changing the API key, endpoint, or model."),
                        List.of("Do not assume the live model works before the connection test passes.", "Changing model settings affects every new reply in the active runtime.", "Capture acceptance evidence after each configuration change.")
                )
        );
    }

    static String blankTo(String value, String fallback) { return value == null || value.isBlank() ? fallback : value.trim(); }
    static String maskApiKey(String apiKey) { if (apiKey == null || apiKey.isBlank()) return ""; return apiKey.length() <= 8 ? "configured" : apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4); }
    static String now() { return Instant.now().truncatedTo(ChronoUnit.SECONDS).toString(); }

    private void seedWorkspace() {
        ProjectState project = newProject("project-loom", "loom", "以项目和会话为中心的 AI 工作台，支持可见 Trace 与模型配置。", "保持会话优先体验、Trace 可见性和文档驱动交付节奏。");
        projects.put(project.id, project);
        fileAssetsByProject.put(project.id, List.of(
                new FileAssetSummaryView("file-prd", project.id, "loom-prd.md", "text/markdown", 18_240, "ready", now()),
                new FileAssetSummaryView("file-contract", project.id, "phase1-contract-freeze.md", "text/markdown", 24_512, "ready", now()),
                new FileAssetSummaryView("file-smoke", project.id, "frontend-smoke-checklist.md", "text/markdown", 6_144, "pending", now())
        ));
        memoryItems.add(new MemoryItemView("memory-project-goal", "project", project.id, null, "交付过程中保持会话、Trace 和项目状态可见。", "explicit", now()));
        memoryItems.add(new MemoryItemView("memory-contract", "project", project.id, null, "模型配置与聊天路由必须和设置页保持一致。", "system", now()));
        seedConversation(project, "conversation-v1", "Phase 1 验收", "验收范围、交付计划与工作台首轮迭代。", "plan", "active", true);
        seedConversation(project, "conversation-trace", "Trace 复盘", "让推理与执行过程保持可见，而不是隐藏在单条回复里。", "review", "idle", true);
        seedConversation(project, "conversation-shell", "工作台壳层", "将占位式 UI 推进为接入真实 API 的项目化工作台。", "chat", "active", false);
        ConversationState seed = conversations.get("conversation-v1");
        seed.messages.add(newMessage(project.id, seed.id, "user", "user", "需要接入真实模型，并把会话流程改成项目归属可切换。", null, null, seed.updatedAt, seed.updatedAt, null));
        seed.messages.add(newMessage(project.id, seed.id, "thinking_summary", "assistant", "先对齐后端设置模型并接入 MiniMax，再把新会话流程切到项目选择。", "先后端，再设置页，再会话流。", "completed", seed.updatedAt, seed.updatedAt, 920L));
        seed.messages.add(newMessage(project.id, seed.id, "assistant", "assistant", "第一轮交付会先提供模型配置、连接测试和按项目归属的会话创建能力。", "设置与会话创建已经是当前关键路径。", "completed", seed.updatedAt, seed.updatedAt, 1540L));
        seed.summary = "真实模型接入与项目归属会话流。";
        seed.traceSummary = "先打通后端模型链路，再把会话流和轨迹在界面里完整呈现。";
        seed.traceSteps.clear();
        seed.traceSteps.addAll(List.of(
                new RunStepView("trace-context-" + seed.id, "run-seed-" + seed.id, "加载上下文", "读取项目说明与最近会话历史。", "success", seed.updatedAt, seed.updatedAt, null),
                new RunStepView("trace-model-" + seed.id, "run-seed-" + seed.id, "调用模型", "使用已配置的模型链路，或在未配置时给出设置引导。", "success", seed.updatedAt, seed.updatedAt, null),
                new RunStepView("trace-publish-" + seed.id, "run-seed-" + seed.id, "发布回复", "持久化可见回复、思考摘要和更新后的轨迹。", "success", seed.updatedAt, seed.updatedAt, null)
        ));
        seed.traceUpdatedAt = seed.updatedAt;
        seed.lastMessageAt = seed.messages.get(seed.messages.size() - 1).completedAt();
        seed.updatedAt = seed.lastMessageAt;
        project.lastMessageAt = seed.lastMessageAt;
        project.updatedAt = seed.updatedAt;
        memoryItems.add(new MemoryItemView("memory-conversation-v1", "conversation", project.id, seed.id, "这条线程是当前真实模型里程碑的验收主线。", "assisted", now()));
    }

    private List<LlmProviderPresetView> providerPresets() {
        return List.of(
                new LlmProviderPresetView(
                        "minimax-cn",
                        "MiniMax (China)",
                        "MiniMax",
                        true,
                        true,
                        "https://api.minimaxi.com/v1",
                        "MiniMax-M2.7",
                        "国内站预设，Loom 默认参数已自动带入。",
                        List.of(
                                new LlmModelOptionView("MiniMax-M2.7", "MiniMax M2.7", "当前主推的 MiniMax 文本模型。"),
                                new LlmModelOptionView("MiniMax-M2.7-highspeed", "MiniMax M2.7 High-Speed", "更低延迟，适合更快的交互响应。")
                        )
                ),
                new LlmProviderPresetView(
                        "minimax-global",
                        "MiniMax (Global)",
                        "MiniMax",
                        true,
                        false,
                        "https://api.minimax.io/v1",
                        "MiniMax-M2.7",
                        "国际站预设，保留 Loom 默认参数并切换到全球端点。",
                        List.of(
                                new LlmModelOptionView("MiniMax-M2.7", "MiniMax M2.7", "当前主推的 MiniMax 文本模型。"),
                                new LlmModelOptionView("MiniMax-M2.7-highspeed", "MiniMax M2.7 High-Speed", "更低延迟，适合更快的交互响应。")
                        )
                ),
                new LlmProviderPresetView(
                        "kimi",
                        "Kimi",
                        "Moonshot AI",
                        true,
                        false,
                        "https://api.moonshot.cn/v1",
                        "kimi-k2-0905-preview",
                        "Official Moonshot OpenAI-compatible preset for Kimi.",
                        List.of(
                                new LlmModelOptionView("kimi-k2-0905-preview", "Kimi K2 0905 Preview", "Official Kimi K2 preview model with strong coding and agentic performance."),
                                new LlmModelOptionView("kimi-k2-turbo-preview", "Kimi K2 Turbo Preview", "A faster Kimi K2 option for low-latency chat turns."),
                                new LlmModelOptionView("moonshot-v1-128k", "Moonshot V1 128K", "Long-context Moonshot model for general drafting and review.")
                        )
                ),
                new LlmProviderPresetView(
                        "openai",
                        "OpenAI",
                        "OpenAI",
                        false,
                        false,
                        "https://api.openai.com/v1",
                        "gpt-5-mini",
                        "预设已预留，后续接入。",
                        List.of()
                ),
                new LlmProviderPresetView(
                        "anthropic",
                        "Anthropic",
                        "Anthropic",
                        false,
                        false,
                        "https://api.anthropic.com/v1",
                        "claude-sonnet-4-5",
                        "预设已预留，后续接入。",
                        List.of()
                ),
                new LlmProviderPresetView(
                        "gemini",
                        "Gemini",
                        "Google",
                        false,
                        false,
                        "https://generativelanguage.googleapis.com",
                        "gemini-2.5-pro",
                        "预设已预留，后续接入。",
                        List.of()
                ),
                new LlmProviderPresetView(
                        "deepseek",
                        "DeepSeek",
                        "DeepSeek",
                        false,
                        false,
                        "https://api.deepseek.com/v1",
                        "deepseek-chat",
                        "预设已预留，后续接入。",
                        List.of()
                )
        );
    }

    private List<LlmConfigState> orderedLlmConfigs() {
        return llmConfigs.values().stream()
                .sorted(Comparator.comparing((LlmConfigState config) -> config.active).reversed().thenComparing(config -> config.updatedAt, Comparator.reverseOrder()))
                .toList();
    }

    private LlmConfigState activeLlmConfig() {
        return orderedLlmConfigs().stream().findFirst().orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "LLM_CONFIG_MISSING", "No LLM configuration is available"));
    }

    private void ensureSingleActiveConfig() {
        List<LlmConfigState> ordered = new ArrayList<>(orderedLlmConfigs());
        if (ordered.isEmpty()) {
            return;
        }

        boolean foundActive = false;
        for (LlmConfigState config : ordered) {
            if (config.active && !foundActive) {
                foundActive = true;
                continue;
            }
            config.active = false;
        }

        if (!foundActive) {
            ordered.get(0).active = true;
        }
    }

    private void activateConfig(String profileId) {
        String timestamp = now();
        llmConfigs.values().forEach(config -> {
            config.active = Objects.equals(config.id, profileId);
            if (config.active) {
                config.updatedAt = timestamp;
            }
        });
    }

    private void persistLlmConfigs() {
        llmSettingsRepository.saveProfiles(new ArrayList<>(llmConfigs.values()));
    }

    private LlmProviderPresetView requirePreset(String presetId) {
        return providerPresets.stream()
                .filter(candidate -> candidate.id().equals(presetId))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "LLM_PRESET_NOT_FOUND", "Unknown LLM preset"));
    }

    private LlmConfigState createConfigFromPreset(LlmProviderPresetView preset) {
        String timestamp = now();
        return new LlmConfigState(
                "profile-" + UUID.randomUUID(),
                preset.id(),
                preset.provider(),
                preset.label(),
                preset.apiBaseUrl(),
                preset.defaultModelId(),
                "",
                "You are Loom, a careful AI teammate that explains decisions clearly and keeps execution visible.",
                1.0d,
                1024,
                60000,
                false,
                timestamp
        );
    }

    private LlmConfigState resolveBaseConfig(UpdateLlmConfigRequest request) {
        if (request != null && request.profileId() != null && !request.profileId().isBlank()) {
            LlmConfigState existing = llmConfigs.get(request.profileId());
            if (existing != null) {
                return existing;
            }
        }

        if (request != null && request.presetId() != null && !request.presetId().isBlank()) {
            return createConfigFromPreset(requirePreset(request.presetId()));
        }

        return activeLlmConfig();
    }

    private LlmConfigState upsertLlmConfig(UpdateLlmConfigRequest request) {
        LlmConfigState base = resolveBaseConfig(request);
        LlmConfigState next = base.copy();
        next.apply(request, providerPresets);
        next.updatedAt = now();
        llmConfigs.put(next.id, next);
        ensureSingleActiveConfig();
        return next;
    }

    private ProjectState newProject(String id, String name, String description, String instructions) {
        String createdAt = now();
        return new ProjectState(id, name, description, "active", instructions, List.of(), new CapabilityBindingSummary(activeLlmConfig().id, skills.stream().map(SkillView::id).toList(), routingPolicy.id()), createdAt, createdAt);
    }

    private void seedConversation(ProjectState project, String id, String title, String summary, String mode, String status, boolean pinned) {
        ConversationState state = new ConversationState(id, project.id, title, mode, pinned, now());
        state.summary = summary;
        state.status = status;
        initializeContext(state);
        state.traceSummary = "Keep execution visible while the session is active.";
        state.traceSteps.addAll(defaultPreviewTraceSteps(id, null, state.updatedAt));
        state.traceUpdatedAt = state.updatedAt;
        conversations.put(id, state);
        if (pinned && !project.pinnedConversationIds.contains(id)) {
            project.pinnedConversationIds.add(id);
        }
    }

    private StreamedAssistantTurn generateAssistantTurn(
            ProjectState project,
            ConversationState conversation,
            PendingAssistantTurn pendingTurn,
            Consumer<Map<String, Object>> sink,
            MessageAccumulator thinkingAccumulator,
            MessageAccumulator assistantAccumulator
    ) {
        LlmConfigState activeConfig = activeLlmConfig();
        if (!activeConfig.isConfigured()) {
            String reasoning = "The request was accepted, but the workspace still needs a live " + activeConfig.provider + " API key.";
            String assistantBody = activeConfig.provider + " is not configured yet. Open Settings > Models, choose a preset, add an API key, and run a connection test before using live chat.";
            appendDeltaAndEmit(project.id, conversation.id, "thinking.summary.delta", thinkingAccumulator, reasoning, sink);
            appendDeltaAndEmit(project.id, conversation.id, "message.delta", assistantAccumulator, assistantBody, sink);
            return new StreamedAssistantTurn(assistantBody, reasoning, "Configuration required", 0L, null);
        }

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage(
                "system",
                activeConfig.systemPrompt
                        + "\n\nProject name: " + project.name
                        + "\nProject instructions: " + project.instructions
        ));
        int start = Math.max(0, conversation.messages.size() - 6);
        for (int index = start; index < conversation.messages.size(); index++) {
            MessageView message = conversation.messages.get(index);
            if ("user".equals(message.role()) && "user".equals(message.kind())) {
                messages.add(new ChatMessage("user", message.body()));
            }
            if ("assistant".equals(message.role()) && "assistant".equals(message.kind())) {
                messages.add(new ChatMessage("assistant", message.body()));
            }
        }
        messages.add(new ChatMessage("user", pendingTurn.userInput));
        ChatCompletionResult result = minimaxChatClient.stream(
                new ChatCompletionRequest(activeConfig.apiBaseUrl, activeConfig.apiKey, activeConfig.modelId, activeConfig.temperature, activeConfig.maxTokens, activeConfig.timeoutMs, messages),
                new MinimaxChatClient.StreamListener() {
                    @Override
                    public void onReasoningDelta(String delta) {
                        appendDeltaAndEmit(project.id, conversation.id, "thinking.summary.delta", thinkingAccumulator, delta, sink);
                    }

                    @Override
                    public void onTextDelta(String delta) {
                        appendDeltaAndEmit(project.id, conversation.id, "message.delta", assistantAccumulator, delta, sink);
                    }
                }
        );
        String reasoningBody = thinkingAccumulator.body.length() == 0 ? result.reasoningSummary() : thinkingAccumulator.body.toString();
        String assistantBody = assistantAccumulator.body.length() == 0 ? result.outputText() : assistantAccumulator.body.toString();
        return new StreamedAssistantTurn(assistantBody, reasoningBody, "Live reply generated through " + result.responseModel() + ".", result.latencyMs(), result.responseModel());
    }

    private ActionState newAction(String projectId, String conversationId, String summary, String status, String startedAt, String completedAt) {
        ActionState state = new ActionState(
                "action-" + UUID.randomUUID(),
                projectId,
                conversationId,
                "run-" + UUID.randomUUID(),
                "message-response",
                status,
                summary,
                startedAt,
                completedAt,
                List.of("trace-context-" + conversationId, "trace-model-" + conversationId, "trace-publish-" + conversationId)
        );
        actions.put(state.id, state);
        return state;
    }

    private ActionView actionView(String actionId) {
        ActionState state = actions.get(actionId);
        if (state == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ACTION_NOT_FOUND", "Action does not exist");
        }
        return state.toView();
    }

    private void initializeContext(ConversationState state) {
        state.context.conversationSummary = "当前会话已准备好收集项目相关意图、轨迹与配置更新。";
        state.context.decisions = List.of("会话保持项目优先归属。", "在设置页展示实时模型状态。");
        state.context.openLoops = List.of("如果需要实时聊天，补齐模型配置。", "补充验收证据。");
        state.context.activeGoals = List.of("先从正确的项目开始。");
        state.context.constraints = List.of("不要隐藏模型状态。", "保持轨迹可见。");
        state.context.references = List.of(new ContextReferenceItem("ref-project", "项目", "memory", "当前需要坚持项目优先的会话流。"));
        state.context.snapshots = List.of(new ContextSnapshotView("snapshot-summary-" + state.id, state.projectId, state.id, "conversation_summary", state.summary, state.updatedAt));
        state.context.updatedAt = state.updatedAt;
    }

    private void updateContextAfterMessage(String projectId, ConversationState state, String latestUserInput, String assistantBody, String updatedAt) {
        state.context.updatedAt = updatedAt;
        state.context.conversationSummary = "会话已经吸收最新用户请求，并更新了当前执行路径。";
        state.context.decisions = List.of("优先使用已配置的模型端点。", "新会话创建始终绑定到所选项目。", "验证完成后刷新验收证据。");
        state.context.openLoops = List.of("配置变更后重新测试模型连通性。", "联动验证项目创建和会话创建。", "确认消息流仍能正确渲染。");
        state.context.activeGoals = List.of("回答当前请求。", summarize(latestUserInput));
        state.context.references = List.of(
                new ContextReferenceItem("ref-latest-message", "Latest message", "conversation", summarize(latestUserInput)),
                new ContextReferenceItem("ref-latest-reply", "Latest reply", "conversation", summarize(assistantBody))
        );
        state.context.snapshots = appendSnapshot(state.context.snapshots, new ContextSnapshotView("snapshot-decision-" + UUID.randomUUID(), projectId, state.id, "decisions", String.join(" | ", state.context.decisions), updatedAt));
    }

    private ContextPanelView assembleContextPanel(ConversationState state) {
        return conversationContextService.assemble(contextInputsFor(state, "active_context", blankTo(state.context.updatedAt, state.updatedAt)));
    }

    private ContextPanelView persistContextSnapshot(ConversationState state, String snapshotKind, String updatedAt) {
        ContextSnapshotView snapshot = conversationContextService.storeSnapshot(contextInputsFor(state, snapshotKind, updatedAt));
        state.context.snapshots = mergeSnapshots(state.context.snapshots, List.of(snapshot));
        return assembleContextPanel(state);
    }

    private ConversationContextInputs contextInputsFor(ConversationState state, String snapshotKind, String updatedAt) {
        return new ConversationContextInputs(
                state.projectId,
                state.id,
                state.context.conversationSummary,
                state.context.decisions,
                state.context.openLoops,
                state.context.activeGoals,
                state.context.constraints,
                state.context.references,
                recentMessages(state, 6),
                memoryItemsForContext(state),
                mergedContextSnapshots(state),
                snapshotKind,
                updatedAt
        );
    }

    private List<MessageView> recentMessages(ConversationState state, int limit) {
        if (state.messages.isEmpty()) {
            return List.of();
        }
        int fromIndex = Math.max(0, state.messages.size() - Math.max(1, limit));
        return List.copyOf(state.messages.subList(fromIndex, state.messages.size()));
    }

    private List<MemoryItemView> memoryItemsForContext(ConversationState state) {
        List<MemoryItemView> persisted = memoryService.listMemory(new MemoryQuery(state.projectId, state.id, "all", null, 100)).items();
        if (!persisted.isEmpty()) {
            return persisted;
        }
        return memoryItems.stream()
                .filter(item -> item.projectId() == null || Objects.equals(item.projectId(), state.projectId))
                .filter(item -> item.conversationId() == null || Objects.equals(item.conversationId(), state.id))
                .sorted(Comparator.comparing(MemoryItemView::updatedAt).reversed())
                .toList();
    }

    private List<ContextSnapshotView> mergedContextSnapshots(ConversationState state) {
        return mergeSnapshots(state.context.snapshots, conversationContextService.listSnapshots(state.projectId, state.id));
    }

    private List<ContextSnapshotView> mergeSnapshots(List<ContextSnapshotView> primary, List<ContextSnapshotView> secondary) {
        LinkedHashMap<String, ContextSnapshotView> merged = new LinkedHashMap<>();
        for (ContextSnapshotView snapshot : primary == null ? List.<ContextSnapshotView>of() : primary) {
            merged.put(snapshot.id(), snapshot);
        }
        for (ContextSnapshotView snapshot : secondary == null ? List.<ContextSnapshotView>of() : secondary) {
            merged.put(snapshot.id(), snapshot);
        }
        return merged.values().stream()
                .sorted(Comparator.comparing(ContextSnapshotView::updatedAt).reversed())
                .limit(12)
                .toList();
    }

    private MemorySuggestionView createMemorySuggestion(String projectId, String conversationId, String userInput, String assistantBody) {
        String content = summarize(userInput) + " -> " + summarize(assistantBody);
        if (content.isBlank()) {
            return null;
        }
        return memoryService.suggestMemory(new MemorySuggestionCommand(projectId, conversationId, "conversation", content));
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
                return summarize(message.body());
            }
        }
        return "No user input yet.";
    }

    private MessageView latestMessageByKind(String projectId, String conversationId, String kind) {
        ConversationState state = requireConversation(projectId, conversationId);
        for (int index = state.messages.size() - 1; index >= 0; index--) {
            MessageView message = state.messages.get(index);
            if (kind.equals(message.kind())) {
                return message;
            }
        }
        return null;
    }

    private <T> CursorPage<T> page(List<T> items, String cursor, Integer limit) {
        int offset = parseCursor(cursor);
        int pageSize = limit == null || limit <= 0 ? 50 : limit;
        if (offset >= items.size()) {
            return new CursorPage<>(List.of(), null, false);
        }
        List<T> page = items.subList(offset, Math.min(items.size(), offset + pageSize));
        boolean hasMore = offset + page.size() < items.size();
        String nextCursor = hasMore ? String.valueOf(offset + page.size()) : null;
        return new CursorPage<>(List.copyOf(page), nextCursor, hasMore);
    }

    private int parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(cursor.trim()));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private boolean matchesMemoryScope(String scope, String conversationId, MemoryItemView item) {
        String normalized = scope == null ? "" : scope.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "conversation" -> conversationId != null && conversationId.equals(item.conversationId());
            case "global" -> "global".equals(item.scope());
            case "all" -> true;
            default -> "project".equals(item.scope()) || "global".equals(item.scope()) || (conversationId != null && conversationId.equals(item.conversationId()));
        };
    }

    private ProjectListItem toProjectListItem(ProjectState state) {
        return new ProjectListItem(state.id, state.name, state.description, state.status, projectConversations(state.id).size(), state.lastMessageAt, state.updatedAt);
    }

    private ProjectView toProjectView(ProjectState state) {
        return new ProjectView(state.id, state.name, state.description, state.status, projectConversations(state.id).size(), state.lastMessageAt, state.updatedAt, state.instructions, List.copyOf(state.pinnedConversationIds), state.bindings);
    }

    private ConversationListItem toConversationListItem(ConversationState state) {
        return new ConversationListItem(state.id, state.projectId, state.title, state.summary, state.mode, state.status, state.pinned, state.updatedAt, state.lastMessageAt);
    }

    private ConversationView toConversationView(ConversationState state) {
        return new ConversationView(state.id, state.projectId, state.title, state.summary, state.mode, state.status, state.pinned, state.updatedAt, state.lastMessageAt, state.context.conversationSummary, state.activeRunId);
    }

    private BootstrapPayload.ConversationSummary toBootstrapConversation(ConversationState state) {
        return new BootstrapPayload.ConversationSummary(state.id, state.title, state.summary, "Updated " + state.updatedAt, state.mode, state.status, state.pinned);
    }

    private String bootstrapMessageLabel(MessageView message) {
        if ("user".equals(message.role())) {
            return "User";
        }
        if ("thinking_summary".equals(message.kind())) {
            return "Thinking";
        }
        return "Assistant";
    }

    private MessageView newMessage(
            String projectId,
            String conversationId,
            String kind,
            String role,
            String body,
            String summary,
            String statusLabel,
            String createdAt,
            String completedAt,
            Long latencyMs
    ) {
        return new MessageView(
                "message-" + UUID.randomUUID(),
                projectId,
                conversationId,
                kind,
                role,
                body,
                summary,
                statusLabel,
                latencyMs,
                messageSeq.incrementAndGet(),
                createdAt,
                completedAt,
                List.of()
        );
    }

    private MessageView newMessageWithId(
            String messageId,
            String projectId,
            String conversationId,
            String kind,
            String role,
            String body,
            String summary,
            String statusLabel,
            String createdAt,
            String completedAt,
            Long latencyMs
    ) {
        return new MessageView(
                messageId,
                projectId,
                conversationId,
                kind,
                role,
                body,
                summary,
                statusLabel,
                latencyMs,
                messageSeq.incrementAndGet(),
                createdAt,
                completedAt,
                List.of()
        );
    }

    private ProjectState requireProject(String projectId) {
        ProjectState state = projects.get(projectId);
        if (state == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project does not exist");
        }
        return state;
    }

    private ConversationState requireConversation(String projectId, String conversationId) {
        ConversationState state = conversations.get(conversationId);
        if (state == null || !state.projectId.equals(projectId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "CONVERSATION_NOT_FOUND", "Conversation does not exist");
        }
        return state;
    }

    private List<ConversationState> projectConversations(String projectId) {
        return conversations.values().stream().filter(conversation -> conversation.projectId.equals(projectId)).toList();
    }

    private String nextProjectId(String rawName) {
        String slug = rawName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        String normalizedSlug = slug.isBlank() ? "project" : slug;
        String base = normalizedSlug.startsWith("project-") || "project".equals(normalizedSlug)
                ? normalizedSlug
                : "project-" + normalizedSlug;
        return projects.containsKey(base) ? base + "-" + projectSeq.incrementAndGet() : base;
    }

    private String nextGeneratedProjectName() {
        return "Project " + (projects.size() + 1);
    }

    private void moveConversationToProject(ConversationState state, ProjectState nextProject) {
        ProjectState previousProject = requireProject(state.projectId);
        previousProject.pinnedConversationIds.remove(state.id);
        nextProject.updatedAt = now();
        if (state.pinned && !nextProject.pinnedConversationIds.contains(state.id)) {
            nextProject.pinnedConversationIds.add(state.id);
        }

        state.projectId = nextProject.id;
        state.messages.replaceAll(message -> new MessageView(
                message.id(),
                nextProject.id,
                message.conversationId(),
                message.kind(),
                message.role(),
                message.body(),
                message.summary(),
                message.statusLabel(),
                message.latencyMs(),
                message.sequence(),
                message.createdAt(),
                message.completedAt(),
                message.attachments()
        ));
        state.context.snapshots = state.context.snapshots.stream()
                .map(snapshot -> new ContextSnapshotView(
                        snapshot.id(),
                        nextProject.id,
                        snapshot.conversationId(),
                        snapshot.kind(),
                        snapshot.content(),
                        snapshot.updatedAt()
                ))
                .toList();

        for (int index = 0; index < memoryItems.size(); index++) {
            MemoryItemView item = memoryItems.get(index);
            if (Objects.equals(item.conversationId(), state.id)) {
                memoryItems.set(index, new MemoryItemView(
                        item.id(),
                        item.scope(),
                        nextProject.id,
                        item.conversationId(),
                        item.content(),
                        item.source(),
                        item.updatedAt()
                ));
            }
        }

        if (state.activeActionId != null) {
            ActionState action = actions.get(state.activeActionId);
            if (action != null) {
                action.projectId = nextProject.id;
            }
        }

        previousProject.updatedAt = now();
        previousProject.lastMessageAt = projectConversations(previousProject.id).stream()
                .map(conversation -> conversation.lastMessageAt)
                .filter(Objects::nonNull)
                .max(String::compareTo)
                .orElse(previousProject.lastMessageAt);
        nextProject.lastMessageAt = state.lastMessageAt;
    }

    private void replayLatestConversationEvents(String projectId, String conversationId, Consumer<Map<String, Object>> sink) {
        String emittedAt = now();
        MessageView thinking = latestMessageByKind(projectId, conversationId, "thinking_summary");
        MessageView assistant = latestMessageByKind(projectId, conversationId, "assistant");
        TracePanelView trace = getTrace(projectId, conversationId);

        appendMessageReplayEvents(projectId, conversationId, "thinking.summary.delta", thinking, sink);
        if (thinking != null) {
            sink.accept(event("thinking.summary.done", projectId, conversationId, emittedAt, Map.of("message", thinking)));
        }

        appendMessageReplayEvents(projectId, conversationId, "message.delta", assistant, sink);
        if (assistant != null) {
            sink.accept(event("message.done", projectId, conversationId, emittedAt, Map.of("message", assistant)));
        }

        for (RunStepView step : trace.steps()) {
            sink.accept(event("trace.step.created", projectId, conversationId, emittedAt, Map.of("runId", trace.activeRun() == null ? "run-preview-" + conversationId : trace.activeRun().id(), "step", step)));
            sink.accept(event(stepCompleted(step.status()) ? "trace.step.completed" : "trace.step.updated", projectId, conversationId, emittedAt, Map.of("runId", trace.activeRun() == null ? "run-preview-" + conversationId : trace.activeRun().id(), "step", step)));
        }

        sink.accept(event("context.updated", projectId, conversationId, emittedAt, Map.of("context", getContext(projectId, conversationId))));
        if (trace.activeRun() != null) {
            if ("failed".equals(trace.activeRun().status())) {
                sink.accept(event("run.failed", projectId, conversationId, emittedAt, Map.of(
                        "run", trace.activeRun(),
                        "error", Map.of("code", "RUN_FAILED", "message", blankTo(trace.reasoningSummary(), "The active run failed"))
                )));
            } else {
                sink.accept(event("run.completed", projectId, conversationId, emittedAt, Map.of("run", trace.activeRun())));
            }
        }
    }

    private void appendMessageReplayEvents(String projectId, String conversationId, String eventName, MessageView message, Consumer<Map<String, Object>> sink) {
        if (message == null || message.body() == null || message.body().isBlank()) {
            return;
        }
        List<String> chunks = chunkMessage(message.body(), 32);
        for (int index = 0; index < chunks.size(); index++) {
            sink.accept(event(eventName, projectId, conversationId, now(), Map.of("messageId", message.id(), "chunk", chunks.get(index), "chunkIndex", index)));
        }
    }

    private void emitCurrentTraceSteps(String projectId, String conversationId, String runId, Consumer<Map<String, Object>> sink) {
        for (RunStepView step : requireConversation(projectId, conversationId).traceSteps) {
            sink.accept(event("trace.step.created", projectId, conversationId, now(), Map.of("runId", runId, "step", step)));
            sink.accept(event(stepCompleted(step.status()) ? "trace.step.completed" : "trace.step.updated", projectId, conversationId, now(), Map.of("runId", runId, "step", step)));
        }
    }

    private void completeContextStep(String projectId, String conversationId, String runId, String completedAt, Consumer<Map<String, Object>> sink) {
        markTraceStep(projectId, conversationId, runId, "trace-context-" + conversationId, "success", "项目上下文、最近消息和运行策略已加载。", completedAt, sink);
        markTraceStep(projectId, conversationId, runId, "trace-model-" + conversationId, "waiting", "等待模型流式返回增量。", null, sink);
    }

    private void markTraceStep(String projectId, String conversationId, String runId, String stepId, String status, String detail, String completedAt, Consumer<Map<String, Object>> sink) {
        ConversationState state = requireConversation(projectId, conversationId);
        RunStepView existing = state.traceSteps.stream().filter(step -> step.id().equals(stepId)).findFirst().orElse(null);
        String startedAt = existing != null && existing.startedAt() != null ? existing.startedAt() : now();
        RunStepView nextStep = new RunStepView(
                stepId,
                runId,
                stepTitleFor(stepId),
                detail,
                status,
                startedAt,
                completedAt,
                "failed".equals(status) ? detail : null
        );
        state.traceSteps.removeIf(step -> step.id().equals(stepId));
        int insertIndex = Math.min(traceIndexFor(stepId), state.traceSteps.size());
        state.traceSteps.add(insertIndex, nextStep);
        state.traceUpdatedAt = now();
        sink.accept(event(
                existing == null ? "trace.step.created" : stepCompleted(status) ? "trace.step.completed" : "trace.step.updated",
                projectId,
                conversationId,
                state.traceUpdatedAt,
                Map.of("runId", runId, "step", nextStep)
        ));
    }

    private String stepTitleFor(String stepId) {
        if (stepId.contains("context")) {
            return "加载上下文";
        }
        if (stepId.contains("model")) {
            return "调用模型";
        }
        return "发布回复";
    }

    private int traceIndexFor(String stepId) {
        if (stepId.contains("context")) {
            return 0;
        }
        if (stepId.contains("model")) {
            return 1;
        }
        return 2;
    }

    private boolean stepCompleted(String status) {
        return "success".equals(status) || "failed".equals(status) || "skipped".equals(status);
    }

    private void appendDeltaAndEmit(String projectId, String conversationId, String eventName, MessageAccumulator accumulator, String delta, Consumer<Map<String, Object>> sink) {
        if (delta == null || delta.isBlank()) {
            return;
        }
        accumulator.body.append(delta);
        sink.accept(event(eventName, projectId, conversationId, now(), Map.of(
                "messageId", accumulator.messageId,
                "chunk", delta,
                "chunkIndex", accumulator.nextChunkIndex++
        )));
    }

    private MessageView finalizeThinkingMessage(String projectId, String conversationId, MessageAccumulator accumulator, StreamedAssistantTurn streamedTurn, String completedAt, long latencyMs) {
        String body = accumulator.body.length() == 0 ? streamedTurn.reasoningSummary() : accumulator.body.toString();
        if (body == null || body.isBlank()) {
            return null;
        }
        return newMessageWithId(
                accumulator.messageId,
                projectId,
                conversationId,
                "thinking_summary",
                "assistant",
                body,
                summarize(body),
                "completed",
                accumulator.createdAt,
                completedAt,
                latencyMs
        );
    }

    private MessageView finalizeAssistantMessage(String projectId, String conversationId, MessageAccumulator accumulator, StreamedAssistantTurn streamedTurn, String completedAt, long latencyMs) {
        String body = accumulator.body.length() == 0 ? streamedTurn.assistantBody() : accumulator.body.toString();
        return newMessageWithId(
                accumulator.messageId,
                projectId,
                conversationId,
                "assistant",
                "assistant",
                body,
                blankTo(streamedTurn.reasoningSummary(), summarize(body)),
                "completed",
                accumulator.createdAt,
                completedAt,
                latencyMs
        );
    }

    private List<RunStepView> defaultPreviewTraceSteps(String conversationId, String runId, String timestamp) {
        String resolvedRunId = runId == null ? "run-preview-" + conversationId : runId;
        return List.of(
                new RunStepView("trace-context-" + conversationId, resolvedRunId, "加载上下文", "读取项目说明、最近消息和模型绑定。", "success", timestamp, timestamp, null),
                new RunStepView("trace-model-" + conversationId, resolvedRunId, "调用模型", "使用已配置的提供方，或给出设置引导。", "success", timestamp, timestamp, null),
                new RunStepView("trace-publish-" + conversationId, resolvedRunId, "发布回复", "持久化可见回复、思考摘要和更新后的上下文。", "success", timestamp, timestamp, null)
        );
    }

    private List<RunStepView> defaultRunningTraceSteps(String conversationId, String runId, String startedAt) {
        return List.of(
                new RunStepView("trace-context-" + conversationId, runId, "加载上下文", "读取项目说明、最近消息和模型绑定。", "running", startedAt, null, null),
                new RunStepView("trace-model-" + conversationId, runId, "调用模型", "等待思考摘要和答案的流式增量。", "pending", null, null, null),
                new RunStepView("trace-publish-" + conversationId, runId, "发布回复", "持久化最终答案和刷新后的上下文。", "pending", null, null, null)
        );
    }

    private void updateTraceSummary(ConversationState state, String summary, String updatedAt) {
        state.traceSummary = summary;
        state.traceUpdatedAt = updatedAt;
    }

    private String toRunStatus(String actionStatus) {
        return switch (blankTo(actionStatus, "pending")) {
            case "running" -> "running";
            case "waiting" -> "waiting";
            case "failed" -> "failed";
            case "cancelled" -> "cancelled";
            default -> "success";
        };
    }

    private List<String> chunkMessage(String body, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        for (int start = 0; start < body.length(); start += chunkSize) {
            int end = Math.min(body.length(), start + chunkSize);
            chunks.add(body.substring(start, end));
        }
        return chunks;
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

    private static String summarize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.length() <= 96 ? value : value.substring(0, 95) + "...";
    }

    private static final class MessageAccumulator {
        private final String kind;
        private final String messageId;
        private final String createdAt;
        private final StringBuilder body = new StringBuilder();
        private int nextChunkIndex = 0;

        private MessageAccumulator(String kind, String messageId, String createdAt) {
            this.kind = kind;
            this.messageId = messageId;
            this.createdAt = createdAt;
        }
    }

    private record StreamedAssistantTurn(String assistantBody, String reasoningSummary, String summaryBadge, long latencyMs, String responseModel) {}
}
