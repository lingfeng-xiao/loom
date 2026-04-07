package com.loom.node.dto;

import java.time.Instant;

public record NodeServiceStatus(
        String name,
        String kind,
        String target,
        String status,
        String detail,
        Instant recordedAt
) {
}
