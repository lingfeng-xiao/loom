package com.loom.server.workspace;

import com.loom.server.api.ApiEnvelope;
import com.loom.server.memory.MemoryItemCommand;
import com.loom.server.memory.MemorySuggestionCommand;
import com.loom.server.memory.MemorySuggestionView;
import com.loom.server.workspace.WorkspaceDtos.ActionView;
import com.loom.server.workspace.WorkspaceDtos.ContextSnapshotView;
import com.loom.server.workspace.WorkspaceDtos.ContextRefreshResponse;
import com.loom.server.workspace.WorkspaceDtos.CreateProjectRequest;
import com.loom.server.workspace.WorkspaceDtos.CreateConversationRequest;
import com.loom.server.workspace.WorkspaceDtos.CursorPage;
import com.loom.server.workspace.WorkspaceDtos.FileAssetSummaryView;
import com.loom.server.workspace.WorkspaceDtos.LlmConnectionTestView;
import com.loom.server.workspace.WorkspaceDtos.MemoryItemView;
import com.loom.server.workspace.WorkspaceDtos.MessageView;
import com.loom.server.workspace.WorkspaceDtos.ProjectListItem;
import com.loom.server.workspace.WorkspaceDtos.ProjectView;
import com.loom.server.workspace.WorkspaceDtos.RunStepView;
import com.loom.server.workspace.WorkspaceDtos.SubmitMessageRequest;
import com.loom.server.workspace.WorkspaceDtos.SubmitMessageResponse;
import com.loom.server.workspace.WorkspaceDtos.UpdateLlmConfigRequest;
import com.loom.server.workspace.WorkspaceDtos.UpdateConversationRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WorkspaceController {

    private final WorkspaceStateService workspaceStateService;

    public WorkspaceController(WorkspaceStateService workspaceStateService) {
        this.workspaceStateService = workspaceStateService;
    }

    @GetMapping("/api/projects")
    public ApiEnvelope<CursorPage<ProjectListItem>> listProjects() {
        return ApiEnvelope.of(workspaceStateService.listProjects());
    }

    @PostMapping("/api/projects")
    public ApiEnvelope<ProjectView> createProject(@RequestBody(required = false) CreateProjectRequest request) {
        return ApiEnvelope.of(workspaceStateService.createProject(request));
    }

    @GetMapping("/api/projects/{projectId}")
    public ApiEnvelope<?> getProject(@PathVariable String projectId) {
        return ApiEnvelope.of(workspaceStateService.getProject(projectId));
    }

    @GetMapping("/api/projects/{projectId}/conversations")
    public ApiEnvelope<?> listConversations(@PathVariable String projectId) {
        return ApiEnvelope.of(workspaceStateService.listConversations(projectId));
    }

    @PostMapping("/api/projects/{projectId}/conversations")
    public ApiEnvelope<?> createConversation(@PathVariable String projectId, @RequestBody(required = false) CreateConversationRequest request) {
        return ApiEnvelope.of(workspaceStateService.createConversation(projectId, request));
    }

    @GetMapping("/api/projects/{projectId}/conversations/{conversationId}")
    public ApiEnvelope<?> getConversation(@PathVariable String projectId, @PathVariable String conversationId) {
        return ApiEnvelope.of(workspaceStateService.getConversation(projectId, conversationId));
    }

    @PatchMapping("/api/projects/{projectId}/conversations/{conversationId}")
    public ApiEnvelope<?> updateConversation(
            @PathVariable String projectId,
            @PathVariable String conversationId,
            @RequestBody(required = false) UpdateConversationRequest request
    ) {
        return ApiEnvelope.of(workspaceStateService.updateConversation(projectId, conversationId, request));
    }

    @GetMapping("/api/projects/{projectId}/conversations/{conversationId}/messages")
    public ApiEnvelope<CursorPage<MessageView>> listMessages(@PathVariable String projectId, @PathVariable String conversationId) {
        return ApiEnvelope.of(workspaceStateService.listMessages(projectId, conversationId));
    }

    @PostMapping("/api/projects/{projectId}/conversations/{conversationId}/messages")
    public ApiEnvelope<SubmitMessageResponse> submitMessage(
            @PathVariable String projectId,
            @PathVariable String conversationId,
            @RequestBody SubmitMessageRequest request
    ) {
        return ApiEnvelope.of(workspaceStateService.submitMessage(projectId, conversationId, request));
    }

    @GetMapping("/api/projects/{projectId}/conversations/{conversationId}/context")
    public ApiEnvelope<?> getContext(@PathVariable String projectId, @PathVariable String conversationId) {
        return ApiEnvelope.of(workspaceStateService.getContext(projectId, conversationId));
    }

    @GetMapping("/api/projects/{projectId}/conversations/{conversationId}/context/snapshots")
    public ApiEnvelope<CursorPage<ContextSnapshotView>> listContextSnapshots(
            @PathVariable String projectId,
            @PathVariable String conversationId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit
    ) {
        return ApiEnvelope.of(workspaceStateService.listContextSnapshots(projectId, conversationId, cursor, limit));
    }

    @PostMapping("/api/projects/{projectId}/conversations/{conversationId}/context/refresh")
    public ApiEnvelope<ContextRefreshResponse> refreshContext(@PathVariable String projectId, @PathVariable String conversationId) {
        return ApiEnvelope.of(workspaceStateService.refreshContext(projectId, conversationId));
    }

    @GetMapping("/api/projects/{projectId}/conversations/{conversationId}/trace")
    public ApiEnvelope<?> getTrace(@PathVariable String projectId, @PathVariable String conversationId) {
        return ApiEnvelope.of(workspaceStateService.getTrace(projectId, conversationId));
    }

    @GetMapping("/api/projects/{projectId}/conversations/{conversationId}/actions/{actionId}")
    public ApiEnvelope<ActionView> getAction(
            @PathVariable String projectId,
            @PathVariable String conversationId,
            @PathVariable String actionId
    ) {
        return ApiEnvelope.of(workspaceStateService.getAction(projectId, conversationId, actionId));
    }

    @GetMapping("/api/projects/{projectId}/conversations/{conversationId}/runs/{runId}")
    public ApiEnvelope<?> getRun(@PathVariable String projectId, @PathVariable String conversationId, @PathVariable String runId) {
        return ApiEnvelope.of(workspaceStateService.getRun(projectId, conversationId, runId));
    }

    @GetMapping("/api/projects/{projectId}/conversations/{conversationId}/runs/{runId}/steps")
    public ApiEnvelope<CursorPage<RunStepView>> listRunSteps(
            @PathVariable String projectId,
            @PathVariable String conversationId,
            @PathVariable String runId
    ) {
        return ApiEnvelope.of(workspaceStateService.listRunSteps(projectId, conversationId, runId));
    }

    @GetMapping("/api/projects/{projectId}/files")
    public ApiEnvelope<CursorPage<FileAssetSummaryView>> listFiles(@PathVariable String projectId) {
        return ApiEnvelope.of(workspaceStateService.listFiles(projectId));
    }

    @GetMapping("/api/projects/{projectId}/memory")
    public ApiEnvelope<CursorPage<MemoryItemView>> listMemory(
            @PathVariable String projectId,
            @RequestParam(required = false) String conversationId,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit
    ) {
        return ApiEnvelope.of(workspaceStateService.listMemory(projectId, conversationId, scope, cursor, limit));
    }

    @PostMapping("/api/projects/{projectId}/memory")
    public ApiEnvelope<MemoryItemView> createMemory(
            @PathVariable String projectId,
            @RequestBody MemoryItemCommand request
    ) {
        return ApiEnvelope.of(workspaceStateService.createMemoryItem(projectId, request));
    }

    @PatchMapping("/api/projects/{projectId}/memory/{memoryId}")
    public ApiEnvelope<MemoryItemView> updateMemory(
            @PathVariable String projectId,
            @PathVariable String memoryId,
            @RequestBody MemoryItemCommand request
    ) {
        return ApiEnvelope.of(workspaceStateService.updateMemoryItem(projectId, memoryId, request));
    }

    @DeleteMapping("/api/projects/{projectId}/memory/{memoryId}")
    public ApiEnvelope<Void> deleteMemory(@PathVariable String projectId, @PathVariable String memoryId) {
        workspaceStateService.deleteMemoryItem(projectId, memoryId);
        return ApiEnvelope.of(null);
    }

    @GetMapping("/api/projects/{projectId}/memory/suggestions")
    public ApiEnvelope<CursorPage<MemorySuggestionView>> listMemorySuggestions(
            @PathVariable String projectId,
            @RequestParam(required = false) String conversationId,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit
    ) {
        return ApiEnvelope.of(workspaceStateService.listMemorySuggestions(projectId, conversationId, scope, cursor, limit));
    }

    @PostMapping("/api/projects/{projectId}/memory/suggestions")
    public ApiEnvelope<MemorySuggestionView> createMemorySuggestion(
            @PathVariable String projectId,
            @RequestBody MemorySuggestionCommand request
    ) {
        return ApiEnvelope.of(workspaceStateService.createMemorySuggestion(projectId, request));
    }

    @PostMapping("/api/projects/{projectId}/memory/suggestions/{suggestionId}/accept")
    public ApiEnvelope<MemorySuggestionView> acceptMemorySuggestion(
            @PathVariable String projectId,
            @PathVariable String suggestionId
    ) {
        return ApiEnvelope.of(workspaceStateService.acceptMemorySuggestion(projectId, suggestionId));
    }

    @PostMapping("/api/projects/{projectId}/memory/suggestions/{suggestionId}/reject")
    public ApiEnvelope<MemorySuggestionView> rejectMemorySuggestion(
            @PathVariable String projectId,
            @PathVariable String suggestionId
    ) {
        return ApiEnvelope.of(workspaceStateService.rejectMemorySuggestion(projectId, suggestionId));
    }

    @GetMapping("/api/settings/overview")
    public ApiEnvelope<?> getSettingsOverview(@RequestParam(defaultValue = "project") String scope) {
        return ApiEnvelope.of(workspaceStateService.getSettingsOverview(scope));
    }

    @GetMapping("/api/capabilities/overview")
    public ApiEnvelope<?> getCapabilitiesOverview(@RequestParam(defaultValue = "project") String scope) {
        return ApiEnvelope.of(workspaceStateService.getCapabilitiesOverview(scope));
    }

    @PostMapping("/api/settings/llm/test")
    public ApiEnvelope<LlmConnectionTestView> testLlmSettings(@RequestBody(required = false) UpdateLlmConfigRequest request) {
        return ApiEnvelope.of(workspaceStateService.testLlmConfiguration(request));
    }

    @PostMapping("/api/settings/llm")
    public ApiEnvelope<?> updateLlmSettings(@RequestBody UpdateLlmConfigRequest request) {
        return ApiEnvelope.of(workspaceStateService.updateLlmConfiguration(request));
    }
}
