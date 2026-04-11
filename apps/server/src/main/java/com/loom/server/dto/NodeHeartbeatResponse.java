package com.loom.server.dto;

import java.time.Instant;

public record NodeHeartbeatResponse(String nodeId, Instant acknowledgedAt) {
}
