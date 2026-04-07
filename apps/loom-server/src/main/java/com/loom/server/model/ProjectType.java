package com.loom.server.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ProjectType {
    KNOWLEDGE("knowledge"),
    OPS("ops"),
    LEARNING("learning");

    private final String value;

    ProjectType(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static ProjectType fromValue(String value) {
        for (ProjectType item : values()) {
            if (item.value.equalsIgnoreCase(value)) {
                return item;
            }
        }
        throw new IllegalArgumentException("Unknown project type: " + value);
    }
}
