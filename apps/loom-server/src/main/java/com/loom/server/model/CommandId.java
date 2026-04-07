package com.loom.server.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CommandId {
    PROJECT_NEW("/project-new"),
    PROJECT_SWITCH("/project-switch"),
    PROJECT_STATUS("/project-status"),
    PLAN("/plan"),
    PLAN_RUN("/plan-run"),
    SAVE_CARD("/save-card"),
    MEMORY_SHOW("/memory-show"),
    MEMORY_SAVE("/memory-save"),
    SKILL_LIST("/skill-list"),
    NODE_STATUS("/node-status"),
    LOGS("/logs");

    private final String value;

    CommandId(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static CommandId fromValue(String value) {
        for (CommandId item : values()) {
            if (item.value.equalsIgnoreCase(value)) {
                return item;
            }
        }
        throw new IllegalArgumentException("Unknown command id: " + value);
    }
}
