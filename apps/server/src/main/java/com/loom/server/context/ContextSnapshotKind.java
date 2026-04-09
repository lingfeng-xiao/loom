package com.loom.server.context;

import java.util.Locale;

public enum ContextSnapshotKind {
    TURN,
    REFRESH,
    MANUAL;

    public static ContextSnapshotKind from(String value) {
        if (value == null || value.isBlank()) {
            return TURN;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "refresh" -> REFRESH;
            case "manual" -> MANUAL;
            case "turn" -> TURN;
            default -> TURN;
        };
    }

    public String wireValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
