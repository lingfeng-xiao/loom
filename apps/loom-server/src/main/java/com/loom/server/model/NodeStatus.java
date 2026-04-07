package com.loom.server.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum NodeStatus {
    ONLINE("online"),
    OFFLINE("offline"),
    DEGRADED("degraded"),
    UNKNOWN("unknown");

    private final String value;

    NodeStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static NodeStatus fromValue(String value) {
        for (NodeStatus item : values()) {
            if (item.value.equalsIgnoreCase(value)) {
                return item;
            }
        }
        throw new IllegalArgumentException("Unknown node status: " + value);
    }
}
