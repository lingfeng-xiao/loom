package com.loom.server.memory;

import com.loom.server.workspace.WorkspaceDtos.MemoryItemView;

public record MemoryItemRecord(
        String id,
        String scope,
        String projectId,
        String conversationId,
        String content,
        String source,
        String updatedAt
) {
    public MemoryItemView toView() {
        return new MemoryItemView(id, scope, projectId, conversationId, content, source, updatedAt);
    }
}
