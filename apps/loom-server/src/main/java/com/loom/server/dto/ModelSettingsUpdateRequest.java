package com.loom.server.dto;

public record ModelSettingsUpdateRequest(
        String providerLabel,
        String baseUrl,
        String model,
        Double temperature
) {
}
