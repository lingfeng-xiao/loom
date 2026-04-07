package com.loom.server.model;

import java.time.Instant;
import java.util.List;

public record Node(
        String id,
        String name,
        NodeType type,
        String host,
        List<String> tags,
        NodeStatus status,
        Instant lastHeartbeat,
        NodeSnapshot snapshot,
        List<String> capabilities,
        String lastError,
        Instant createdAt,
        Instant updatedAt
) {
}
