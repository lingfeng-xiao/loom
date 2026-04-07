package com.template.server.model;

import java.time.Instant;
import java.util.List;

public record NodeRecord(
        String id,
        String name,
        String type,
        String host,
        ProbeStatus status,
        List<String> tags,
        List<String> capabilities,
        Instant lastHeartbeat,
        Instant createdAt,
        Instant updatedAt,
        List<ProbeRecord> probes
) {
}
