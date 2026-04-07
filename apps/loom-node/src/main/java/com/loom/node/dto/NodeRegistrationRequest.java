package com.loom.node.dto;

import java.util.List;

public record NodeRegistrationRequest(
        String name,
        String type,
        String host,
        List<String> tags,
        List<String> capabilities,
        List<String> serviceNames
) {
}
