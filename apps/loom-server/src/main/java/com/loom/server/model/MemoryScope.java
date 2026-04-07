package com.loom.server.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MemoryScope {
    GLOBAL("global"),
    PROJECT("project"),
    DERIVED("derived");

    private final String value;

    MemoryScope(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static MemoryScope fromValue(String value) {
        for (MemoryScope item : values()) {
            if (item.value.equalsIgnoreCase(value)) {
                return item;
            }
        }
        throw new IllegalArgumentException("Unknown memory scope: " + value);
    }
}
