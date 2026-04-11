package com.loom.node.model;

import java.time.Instant;

public record NodeProbeResult(
        String name,
        String kind,
        String target,
        ProbeStatus status,
        String detail,
        Instant recordedAt
) {
}
