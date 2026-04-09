package com.loom.server.memory;

import java.util.Locale;

public enum MemorySuggestionStatus {
    PENDING,
    ACCEPTED,
    REJECTED;

    public static MemorySuggestionStatus from(String value) {
        if (value == null || value.isBlank()) {
            return PENDING;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "accepted" -> ACCEPTED;
            case "rejected" -> REJECTED;
            default -> PENDING;
        };
    }

    public String wireValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
