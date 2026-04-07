package com.loom.server.model;

import java.time.Instant;

public record Conversation(
        String id,
        String projectId,
        String title,
        ConversationMode mode,
        ConversationStatus status,
        String summary,
        Instant createdAt,
        Instant updatedAt
) {
}
