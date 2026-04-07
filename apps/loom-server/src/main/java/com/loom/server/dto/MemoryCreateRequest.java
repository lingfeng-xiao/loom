package com.loom.server.dto;

import com.loom.server.model.MemoryScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MemoryCreateRequest(
        @NotNull MemoryScope scope,
        String projectId,
        @NotBlank String title,
        @NotBlank String content,
        Integer priority,
        String sourceType,
        String sourceRef
) {
}
