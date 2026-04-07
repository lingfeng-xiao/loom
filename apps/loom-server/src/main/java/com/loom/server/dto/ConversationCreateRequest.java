package com.loom.server.dto;

import com.loom.server.model.ConversationMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConversationCreateRequest(
        @NotBlank String title,
        @NotNull ConversationMode mode,
        String summary
) {
}
