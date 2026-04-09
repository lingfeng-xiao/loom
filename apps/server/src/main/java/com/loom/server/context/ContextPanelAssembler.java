package com.loom.server.context;

import com.loom.server.workspace.WorkspaceDtos.ContextPanelView;
import com.loom.server.workspace.WorkspaceDtos.ContextReferenceItem;
import com.loom.server.workspace.WorkspaceDtos.ContextSnapshotView;
import com.loom.server.workspace.WorkspaceDtos.MemoryItemView;
import com.loom.server.workspace.WorkspaceDtos.MessageView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

final class ContextPanelAssembler {
    ContextPanelView assemble(ConversationContextInputs inputs, List<ContextSnapshotView> snapshots) {
        List<String> decisions = normalize(inputs.decisions());
        List<String> openLoops = normalize(inputs.openLoops());
        List<String> activeGoals = normalize(inputs.activeGoals());
        List<String> constraints = normalize(inputs.constraints());
        List<ContextReferenceItem> references = inputs.references() == null ? List.of() : List.copyOf(inputs.references());
        List<ContextSnapshotView> orderedSnapshots = snapshots == null
                ? List.of()
                : snapshots.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ContextSnapshotView::updatedAt).reversed())
                .toList();
        String updatedAt = inputs.updatedAt() == null || inputs.updatedAt().isBlank()
                ? orderedSnapshots.stream().map(ContextSnapshotView::updatedAt).findFirst().orElseGet(() -> "1970-01-01T00:00:00Z")
                : inputs.updatedAt().trim();
        return new ContextPanelView(
                blankTo(inputs.conversationSummary(), "This conversation is ready."),
                decisions,
                openLoops,
                activeGoals,
                constraints,
                references,
                orderedSnapshots,
                updatedAt
        );
    }

    String renderSnapshotContent(ConversationContextInputs inputs, List<ContextSnapshotView> snapshots) {
        StringBuilder builder = new StringBuilder();
        builder.append("kind: ").append(ContextSnapshotKind.from(inputs.snapshotKind()).wireValue()).append('\n');
        builder.append("projectId: ").append(blankTo(inputs.projectId(), "unknown")).append('\n');
        builder.append("conversationId: ").append(blankTo(inputs.conversationId(), "unknown")).append('\n');
        builder.append("updatedAt: ").append(blankTo(inputs.updatedAt(), "unknown")).append('\n');
        builder.append("conversationSummary: ").append(blankTo(inputs.conversationSummary(), "Waiting for the next turn.")).append('\n');
        appendSection(builder, "decisions", normalize(inputs.decisions()));
        appendSection(builder, "openLoops", normalize(inputs.openLoops()));
        appendSection(builder, "activeGoals", normalize(inputs.activeGoals()));
        appendSection(builder, "constraints", normalize(inputs.constraints()));
        appendReferences(builder, inputs.references());
        appendMessages(builder, inputs.recentMessages());
        appendMemory(builder, inputs.memoryItems());
        appendSnapshotSummaries(builder, snapshots);
        return builder.toString();
    }

    private void appendSection(StringBuilder builder, String title, List<String> values) {
        builder.append(title).append(':').append('\n');
        if (values.isEmpty()) {
            builder.append("- none\n");
            return;
        }
        for (String value : values) {
            builder.append("- ").append(value).append('\n');
        }
    }

    private void appendReferences(StringBuilder builder, List<ContextReferenceItem> references) {
        builder.append("references:").append('\n');
        if (references == null || references.isEmpty()) {
            builder.append("- none\n");
            return;
        }
        for (ContextReferenceItem reference : references) {
            builder.append("- ")
                    .append(blankTo(reference.label(), "reference"))
                    .append(" | ")
                    .append(blankTo(reference.kind(), "unknown"))
                    .append(" | ")
                    .append(blankTo(reference.summary(), ""))
                    .append('\n');
        }
    }

    private void appendMessages(StringBuilder builder, List<MessageView> messages) {
        builder.append("recentMessages:").append('\n');
        if (messages == null || messages.isEmpty()) {
            builder.append("- none\n");
            return;
        }
        for (MessageView message : messages) {
            builder.append("- ")
                    .append(blankTo(message.role(), message.kind()))
                    .append(" | ")
                    .append(blankTo(message.summary(), message.body()))
                    .append('\n');
        }
    }

    private void appendMemory(StringBuilder builder, List<MemoryItemView> memoryItems) {
        builder.append("memoryItems:").append('\n');
        if (memoryItems == null || memoryItems.isEmpty()) {
            builder.append("- none\n");
            return;
        }
        for (MemoryItemView memoryItem : memoryItems) {
            builder.append("- ")
                    .append(blankTo(memoryItem.scope(), "project"))
                    .append(" | ")
                    .append(blankTo(memoryItem.source(), "explicit"))
                    .append(" | ")
                    .append(blankTo(memoryItem.content(), ""))
                    .append('\n');
        }
    }

    private void appendSnapshotSummaries(StringBuilder builder, List<ContextSnapshotView> snapshots) {
        builder.append("existingSnapshots:").append('\n');
        if (snapshots == null || snapshots.isEmpty()) {
            builder.append("- none\n");
            return;
        }
        for (ContextSnapshotView snapshot : snapshots.stream().limit(5).toList()) {
            builder.append("- ")
                    .append(blankTo(snapshot.kind(), "turn"))
                    .append(" | ")
                    .append(blankTo(snapshot.updatedAt(), ""))
                    .append(" | ")
                    .append(firstLine(snapshot.content()))
                    .append('\n');
        }
    }

    private List<String> normalize(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private String firstLine(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        int index = value.indexOf('\n');
        return index < 0 ? value.trim() : value.substring(0, index).trim();
    }

    private String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
