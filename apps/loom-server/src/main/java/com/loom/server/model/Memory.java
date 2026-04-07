package com.loom.server.model;

import java.time.Instant;

public record Memory(
        String id,
        MemoryScope scope,
        String projectId,
        String title,
        String content,
        int priority,
        MemoryStatus status,
        String sourceType,
        String sourceRef,
        Instant createdAt,
        Instant updatedAt
) {
}
