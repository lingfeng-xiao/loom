package com.loom.server.model;

import java.util.List;

public record BootstrapPayload(
        String appName,
        String description,
        ProjectSummary project,
        List<WorkspacePageLink> pages,
        List<ConversationSummary> recentConversations,
        List<ConversationSummary> pinnedConversations,
        List<WorkspaceModeOption> modes,
        String activeMode,
        String conversationTitle,
        String conversationMeta,
        List<ConversationMessage> messages,
        ComposerState composer,
        String traceSummary,
        List<TraceStep> traceSteps,
        List<ContextBlock> contextBlocks,
        CapabilitiesOverview capabilities,
        OpenClawOverview openClaw,
        SettingsOverview settings
) {
    public record ProjectSummary(
            String id,
            String name,
            String eyebrow,
            String description,
            String workspaceLabel,
            String lastUpdatedLabel,
            String openClawStatus
    ) {
    }

    public record WorkspacePageLink(
            String id,
            String label,
            String description,
            String shortcut,
            boolean available
    ) {
    }

    public record ConversationSummary(
            String id,
            String title,
            String summary,
            String lastUpdatedLabel,
            String mode,
            String status,
            boolean pinned
    ) {
    }

    public record WorkspaceModeOption(
            String id,
            String label,
            String description
    ) {
    }

    public record ConversationMessage(
            String id,
            String kind,
            String label,
            String body,
            String emphasis,
            String statusLabel
    ) {
    }

    public record ComposerToggle(
            String label,
            boolean enabled
    ) {
    }

    public record ComposerState(
            String placeholder,
            String primaryActionLabel,
            List<String> secondaryActions,
            List<ComposerToggle> toggles
    ) {
    }

    public record TraceStep(
            String id,
            String label,
            String detail,
            String status
    ) {
    }

    public record ContextBlock(
            String id,
            String label,
            String value
    ) {
    }

    public record OverviewCard(
            String id,
            String title,
            String summary,
            List<String> items
    ) {
    }

    public record DetailItem(
            String label,
            String value
    ) {
    }

    public record StatusItem(
            String label,
            String value,
            String tone
    ) {
    }

    public record CapabilitiesOverview(
            String summary,
            List<OverviewCard> cards,
            List<StatusItem> bindingRules
    ) {
    }

    public record OpenClawOverview(
            String summary,
            List<DetailItem> connection,
            List<StatusItem> discovery,
            List<StatusItem> routing,
            List<StatusItem> recentActivity,
            List<String> linkedConversations
    ) {
    }

    public record SettingsOverview(
            String summary,
            List<String> tabs,
            List<DetailItem> profile,
            List<String> guidance,
            List<String> riskNotes
    ) {
    }
}
