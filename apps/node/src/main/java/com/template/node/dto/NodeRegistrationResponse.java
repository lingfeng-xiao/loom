package com.template.node.dto;

import java.time.Instant;

public record NodeRegistrationResponse(String nodeId, Instant registeredAt) {
}
