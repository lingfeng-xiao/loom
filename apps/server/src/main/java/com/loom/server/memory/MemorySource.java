package com.loom.server.memory;

import java.util.Locale;

public enum MemorySource {
    EXPLICIT,
    ASSISTED,
    SYSTEM;

    public static MemorySource from(String value) {
        if (value == null || value.isBlank()) {
            return EXPLICIT;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "assisted" -> ASSISTED;
            case "system" -> SYSTEM;
            default -> EXPLICIT;
        };
    }

    public String wireValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
