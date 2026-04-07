package com.loom.server.model;

import java.time.Instant;
import java.util.List;

public record WorkspaceSettings(
        String workspaceName,
        String language,
        String density,
        String defaultProjectId,
        String defaultLandingView,
        boolean inspectorDefaultOpen,
        ModelSettings model,
        VaultSettings vault,
        WorkspaceNodeSettings nodes,
        AppearanceSettings appearance,
        List<CommandId> enabledCommands,
        List<SkillId> enabledSkills,
        Instant updatedAt
) {
}
