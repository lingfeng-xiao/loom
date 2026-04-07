package com.loom.server.dto;

import com.loom.server.model.CommandId;
import com.loom.server.model.ProjectType;
import com.loom.server.model.SkillId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ProjectCreateRequest(
        @NotBlank String name,
        @NotNull ProjectType type,
        String description,
        List<SkillId> defaultSkills,
        List<CommandId> defaultCommands,
        List<String> boundNodeIds,
        List<String> knowledgeRoots
) {
}
