package com.loom.node.dto;

import java.time.Instant;
import java.util.List;

public record NodeRuntimeStatus(
        String nodeId,
        String nodeName,
        String nodeType,
        String serverBaseUrl,
        boolean registered,
        Instant lastRegisteredAt,
        Instant lastHeartbeatAt,
        String lastError,
        String stateFilePath,
        NodeSnapshot lastSnapshot,
        List<NodeServiceStatus> serviceStatuses
) {
}
