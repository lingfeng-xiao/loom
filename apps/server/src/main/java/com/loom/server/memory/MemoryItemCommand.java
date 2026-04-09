package com.loom.server.memory;

public record MemoryItemCommand(
        String projectId,
        String conversationId,
        String scope,
        String content,
        String source
) {
}
