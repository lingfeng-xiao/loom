package com.loom.server.model;

import java.time.Instant;

public record Message(
        String id,
        String conversationId,
        String projectId,
        String role,
        String content,
        Instant createdAt
) {
}
