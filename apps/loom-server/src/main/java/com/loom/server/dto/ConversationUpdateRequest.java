package com.loom.server.dto;

import com.loom.server.model.ConversationStatus;

public record ConversationUpdateRequest(
        String title,
        ConversationStatus status,
        String summary
) {
}
