package com.loom.server.dto;

import java.util.List;

public record HeartbeatRequest(
        List<String> tags,
        List<String> capabilities
) {
}
