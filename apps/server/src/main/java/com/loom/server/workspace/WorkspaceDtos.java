package com.loom.server.workspace;

import java.util.List;

public final class WorkspaceDtos {

    private WorkspaceDtos() {
    }

    public record CursorPage<T>(List<T> items, String nextCursor, boolean hasMore) {
    }

    public record CapabilityBindingSummary(String defaultModelProfileId, List<String> enabledSkillIds, String defaultRoutingPolicyId) {
    }

    public record ProjectListItem(String id, String name, String description, String status, int conversationCount, String lastMessageAt, String updatedAt) {
    }

    public record ProjectView(
            String id,
            String name,
            String description,
            String status,
            int conversationCount,
            String lastMessageAt,
            String updatedAt,
            String instructions,
            List<String> pinnedConversationIds,
            CapabilityBindingSummary capabilityBindings
    ) {
    }

    public record CreateProjectRequest(String name, String description, String instructions) {
    }

    public record UpdateProjectRequest(String name, String description, String instructions, String status) {
    }

    public record ConversationListItem(
            String id,
            String projectId,
            String title,
            String summary,
            String mode,
            String status,
            boolean pinned,
            String updatedAt,
            String lastMessageAt
    ) {
    }

    public record ConversationView(
            String id,
            String projectId,
            String title,
            String summary,
            String mode,
            String status,
            boolean pinned,
            String updatedAt,
            String lastMessageAt,
            String contextSummary,
            String activeRunId
    ) {
    }

    public record CreateConversationRequest(String title, String mode) {
    }

    public record UpdateConversationRequest(String title, String mode, String status, Boolean pinned) {
    }

    public record MessageAttachmentRef(String fileAssetId, String displayName, String mimeType) {
    }

    public record MessageView(
            String id,
            String projectId,
            String conversationId,
            String kind,
            String role,
            String body,
            String summary,
            String statusLabel,
            int sequence,
            String createdAt,
            String completedAt,
            List<MessageAttachmentRef> attachments
    ) {
    }

    public record SubmitMessageRequest(
            String body,
            String clientMessageId,
            String requestedMode,
            List<String> attachmentIds,
            Boolean allowActions,
            Boolean allowMemory
    ) {
    }

    public record SubmitMessageResponse(String conversationId, MessageView userMessage, String acceptedRunId, String streamPath) {
    }

    public record ContextReferenceItem(String id, String label, String kind, String summary) {
    }

    public record ContextSnapshotView(String id, String projectId, String conversationId, String kind, String content, String updatedAt) {
    }

    public record ContextPanelView(
            String conversationSummary,
            List<String> decisions,
            List<String> openLoops,
            List<String> activeGoals,
            List<String> constraints,
            List<ContextReferenceItem> references,
            List<ContextSnapshotView> snapshots,
            String updatedAt
    ) {
    }

    public record ContextRefreshResponse(ContextPanelView context) {
    }

    public record RunStepView(
            String id,
            String runId,
            String title,
            String detail,
            String status,
            String startedAt,
            String completedAt,
            String errorMessage
    ) {
    }

    public record RunView(
            String id,
            String actionId,
            String projectId,
            String conversationId,
            String status,
            String startedAt,
            String completedAt,
            String externalReference
    ) {
    }

    public record TracePanelView(String reasoningSummary, RunView activeRun, List<RunStepView> steps, String updatedAt) {
    }

    public record FileAssetSummaryView(
            String id,
            String projectId,
            String displayName,
            String mimeType,
            long sizeBytes,
            String parseStatus,
            String uploadedAt
    ) {
    }

    public record MemoryItemView(
            String id,
            String scope,
            String projectId,
            String conversationId,
            String content,
            String source,
            String updatedAt
    ) {
    }

    public record ModelProfileView(
            String id,
            String scope,
            String name,
            String provider,
            String modelId,
            boolean supportsStreaming,
            boolean supportsImages,
            boolean supportsTools,
            boolean supportsLongContext,
            boolean supportsReasoningSummary,
            int timeoutMs
    ) {
    }

    public record SkillView(String id, String scope, String name, boolean enabled, String source) {
    }

    public record McpServerView(String id, String scope, String name, String status, int resourceCount, int promptCount, int toolCount) {
    }

    public record MemoryPolicyView(String id, String scope, boolean autoSuggest, boolean autoPromoteConversationSummary, boolean allowSystemWrites) {
    }

    public record RoutingPolicyView(String id, String scope, String defaultRuntime, boolean allowExternalExecutors, String externalExecutorLabel) {
    }

    public record SettingsOverviewView(
            String activeScope,
            List<String> tabs,
            List<ModelProfileView> modelProfiles,
            List<SkillView> skills,
            List<McpServerView> mcpServers,
            MemoryPolicyView memoryPolicy,
            RoutingPolicyView routingPolicy
    ) {
    }

    public record CapabilityOverviewView(
            String activeScope,
            String summary,
            List<CapabilityCardView> cards,
            List<CapabilityBindingRuleView> bindingRules
    ) {
    }

    public record CapabilityCardView(String id, String title, String summary, List<String> items) {
    }

    public record CapabilityBindingRuleView(String label, String value, String tone) {
    }
}
