package com.loom.node.dto;

public record NodeHeartbeatResponse(
        String nodeId,
        String status,
        String message
) {
}
