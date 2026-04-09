package com.loom.server.memory;

import java.util.Locale;

public enum MemoryScope {
    GLOBAL,
    PROJECT,
    CONVERSATION;

    public static MemoryScope from(String value) {
        if (value == null || value.isBlank()) {
            return PROJECT;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "global" -> GLOBAL;
            case "conversation" -> CONVERSATION;
            case "project" -> PROJECT;
            case "all" -> PROJECT;
            default -> PROJECT;
        };
    }

    public String wireValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
