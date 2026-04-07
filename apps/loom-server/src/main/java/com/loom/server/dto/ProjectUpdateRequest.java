package com.loom.server.dto;

import com.loom.server.model.CommandId;
import com.loom.server.model.ProjectType;
import com.loom.server.model.SkillId;
import java.util.List;

public record ProjectUpdateRequest(
        String name,
        ProjectType type,
        String description,
        List<SkillId> defaultSkills,
        List<CommandId> defaultCommands,
        List<String> boundNodeIds,
        List<String> knowledgeRoots,
        List<String> projectMemoryRefs
) {
}
