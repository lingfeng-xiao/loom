package com.loom.server.model;

import java.time.Instant;

public record AuditLogEntry(
        String id,
        String actor,
        String source,
        String targetType,
        String targetId,
        String action,
        String payloadHash,
        String result,
        Instant createdAt
) {
}
