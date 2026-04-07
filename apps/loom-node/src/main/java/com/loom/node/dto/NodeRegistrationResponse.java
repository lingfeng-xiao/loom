package com.loom.node.dto;

public record NodeRegistrationResponse(
        String nodeId,
        String status,
        String message
) {
}
