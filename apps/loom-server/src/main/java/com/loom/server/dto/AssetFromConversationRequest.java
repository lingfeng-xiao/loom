package com.loom.server.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record AssetFromConversationRequest(
        @NotBlank String conversationId,
        @NotBlank String title,
        @NotBlank String content,
        List<String> tags
) {
}
