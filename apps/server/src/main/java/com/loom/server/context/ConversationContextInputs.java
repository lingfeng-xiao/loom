package com.loom.server.context;

import com.loom.server.workspace.WorkspaceDtos.ContextReferenceItem;
import com.loom.server.workspace.WorkspaceDtos.ContextSnapshotView;
import com.loom.server.workspace.WorkspaceDtos.MemoryItemView;
import com.loom.server.workspace.WorkspaceDtos.MessageView;

import java.util.List;

public record ConversationContextInputs(
        String projectId,
        String conversationId,
        String conversationSummary,
        List<String> decisions,
        List<String> openLoops,
        List<String> activeGoals,
        List<String> constraints,
        List<ContextReferenceItem> references,
        List<MessageView> recentMessages,
        List<MemoryItemView> memoryItems,
        List<ContextSnapshotView> snapshots,
        String snapshotKind,
        String updatedAt
) {
}
