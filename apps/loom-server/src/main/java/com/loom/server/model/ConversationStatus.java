package com.loom.server.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ConversationStatus {
    ACTIVE("active"),
    ARCHIVED("archived");

    private final String value;

    ConversationStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static ConversationStatus fromValue(String value) {
        for (ConversationStatus item : values()) {
            if (item.value.equalsIgnoreCase(value)) {
                return item;
            }
        }
        throw new IllegalArgumentException("Unknown conversation status: " + value);
    }
}
