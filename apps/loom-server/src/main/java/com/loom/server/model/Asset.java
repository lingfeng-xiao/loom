package com.loom.server.model;

import java.time.Instant;
import java.util.List;

public record Asset(
        String id,
        String projectId,
        AssetType type,
        String title,
        String contentRef,
        String storagePath,
        String sourceConversationId,
        String sourcePlanId,
        String sourceNodeId,
        List<String> tags,
        Instant createdAt
) {
}
