package com.loom.server.memory;

public record MemorySuggestionPublishedEvent(
        String projectId,
        String conversationId,
        MemorySuggestionView suggestion
) {
}
