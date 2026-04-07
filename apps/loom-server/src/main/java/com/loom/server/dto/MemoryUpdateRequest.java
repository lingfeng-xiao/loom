package com.loom.server.dto;

import com.loom.server.model.MemoryScope;
import com.loom.server.model.MemoryStatus;

public record MemoryUpdateRequest(
        MemoryScope scope,
        String projectId,
        String title,
        String content,
        Integer priority,
        MemoryStatus status,
        String sourceType,
        String sourceRef
) {
}
