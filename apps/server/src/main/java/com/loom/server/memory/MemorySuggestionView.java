package com.loom.server.memory;

public record MemorySuggestionView(
        String id,
        String scope,
        String status,
        String content,
        String createdAt,
        String projectId,
        String conversationId,
        String updatedAt
) {
}
