package com.loom.node.dto;

import java.time.Instant;

public record NodeHeartbeatResponse(String nodeId, Instant acknowledgedAt) {
}
