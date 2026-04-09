package com.loom.server.memory;

public record MemorySuggestionRecord(
        String id,
        String scope,
        String projectId,
        String conversationId,
        String content,
        String status,
        String createdAt,
        String updatedAt
) {
    public MemorySuggestionView toView() {
        return new MemorySuggestionView(id, scope, status, content, createdAt, projectId, conversationId, updatedAt);
    }
}
