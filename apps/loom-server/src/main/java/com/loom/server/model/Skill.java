package com.loom.server.model;

import java.time.Instant;
import java.util.List;

public record Skill(
        SkillId id,
        String name,
        String version,
        String description,
        String triggerMode,
        String instructionRef,
        String resourceRef,
        List<String> toolBindings,
        String scope,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}
