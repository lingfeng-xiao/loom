package com.loom.server.model;

public record ModelSettings(
        String providerLabel,
        String baseUrl,
        String model,
        double temperature
) {
}
