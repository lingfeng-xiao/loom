package com.loom.node.dto;

import java.time.Instant;

public record NodeHeartbeatRequest(
        String nodeId,
        String nodeName,
        NodeSnapshot snapshot,
        Instant sentAt,
        String lastError
) {
}
