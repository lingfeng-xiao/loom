package com.loom.server.memory;

public record MemoryQuery(
        String projectId,
        String conversationId,
        String scope,
        String cursor,
        Integer limit
) {
}
