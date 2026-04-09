package com.loom.server.memory;

public record MemorySuggestionCommand(
        String projectId,
        String conversationId,
        String scope,
        String content
) {
}
