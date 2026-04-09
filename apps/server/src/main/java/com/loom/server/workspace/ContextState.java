package com.loom.server.workspace;

import com.loom.server.workspace.WorkspaceDtos.ContextReferenceItem;
import com.loom.server.workspace.WorkspaceDtos.ContextSnapshotView;

import java.util.List;

final class ContextState {
    String conversationSummary = "This conversation is ready.";
    List<String> decisions = List.of();
    List<String> openLoops = List.of();
    List<String> activeGoals = List.of();
    List<String> constraints = List.of();
    List<ContextReferenceItem> references = List.of();
    List<ContextSnapshotView> snapshots = List.of();
    String updatedAt = WorkspaceStateService.now();
    int refreshCount = 0;
}
