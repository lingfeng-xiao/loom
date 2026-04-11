package com.loom.server.dto;

import java.time.Instant;

public record NodeRegistrationResponse(String nodeId, Instant registeredAt) {
}
