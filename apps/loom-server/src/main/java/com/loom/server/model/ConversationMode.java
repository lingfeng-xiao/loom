package com.loom.server.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ConversationMode {
    CHAT("chat"),
    PLAN("plan");

    private final String value;

    ConversationMode(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static ConversationMode fromValue(String value) {
        for (ConversationMode item : values()) {
            if (item.value.equalsIgnoreCase(value)) {
                return item;
            }
        }
        throw new IllegalArgumentException("Unknown conversation mode: " + value);
    }
}
