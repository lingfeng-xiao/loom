package com.loom.server.dto;

import com.loom.server.model.NodeSnapshot;
import com.loom.server.model.NodeStatus;
import java.time.Instant;

public record NodeHeartbeatRequest(
        String nodeId,
        String nodeName,
        NodeSnapshot snapshot,
        Instant sentAt,
        NodeStatus status,
        String lastError
) {
}
