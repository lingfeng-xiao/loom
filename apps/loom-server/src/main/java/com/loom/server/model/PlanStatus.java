package com.loom.server.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PlanStatus {
    DRAFT("draft"),
    READY("ready"),
    APPROVED("approved"),
    RUNNING("running"),
    COMPLETED("completed"),
    FAILED("failed");

    private final String value;

    PlanStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static PlanStatus fromValue(String value) {
        for (PlanStatus item : values()) {
            if (item.value.equalsIgnoreCase(value)) {
                return item;
            }
        }
        throw new IllegalArgumentException("Unknown plan status: " + value);
    }
}
