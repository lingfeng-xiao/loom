package com.loom.server.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AssetType {
    KNOWLEDGE_CARD("knowledge_card"),
    OPS_NOTE("ops_note"),
    LEARNING_CARD("learning_card"),
    SUMMARY_NOTE("summary_note"),
    STRUCTURED_MARKDOWN("structured_markdown");

    private final String value;

    AssetType(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static AssetType fromValue(String value) {
        for (AssetType item : values()) {
            if (item.value.equalsIgnoreCase(value)) {
                return item;
            }
        }
        throw new IllegalArgumentException("Unknown asset type: " + value);
    }
}
