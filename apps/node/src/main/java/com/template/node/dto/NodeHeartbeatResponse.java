package com.template.node.dto;

import java.time.Instant;

public record NodeHeartbeatResponse(String nodeId, Instant acknowledgedAt) {
}
