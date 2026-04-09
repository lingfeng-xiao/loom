package com.loom.server.context;

import com.loom.server.workspace.WorkspaceDtos.ContextSnapshotView;

public record ContextSnapshotRecord(
        String id,
        String projectId,
        String conversationId,
        String kind,
        String content,
        String updatedAt
) {
    public ContextSnapshotView toView() {
        return new ContextSnapshotView(id, projectId, conversationId, kind, content, updatedAt);
    }
}
