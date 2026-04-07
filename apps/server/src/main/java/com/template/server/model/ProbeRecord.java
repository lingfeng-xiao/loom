package com.template.server.model;

import java.time.Instant;

public record ProbeRecord(
        String name,
        String kind,
        String target,
        ProbeStatus status,
        String detail,
        Instant recordedAt
) {
}
