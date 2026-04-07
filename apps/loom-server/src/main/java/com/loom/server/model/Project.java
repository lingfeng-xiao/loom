package com.loom.server.model;

import java.time.Instant;
import java.util.List;

public record Project(
        String id,
        String name,
        ProjectType type,
        String description,
        List<SkillId> defaultSkills,
        List<CommandId> defaultCommands,
        List<String> boundNodeIds,
        List<String> knowledgeRoots,
        List<String> projectMemoryRefs,
        Instant createdAt,
        Instant updatedAt
) {
}
