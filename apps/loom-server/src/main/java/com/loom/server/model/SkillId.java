package com.loom.server.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SkillId {
    KNOWLEDGE_CARD_GENERATOR("knowledge-card-generator"),
    OPS_SUMMARY_GENERATOR("ops-summary-generator"),
    OBSIDIAN_NOTE_WRITER("obsidian-note-writer");

    private final String value;

    SkillId(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static SkillId fromValue(String value) {
        for (SkillId item : values()) {
            if (item.value.equalsIgnoreCase(value)) {
                return item;
            }
        }
        throw new IllegalArgumentException("未知技能 ID: " + value);
    }
}
