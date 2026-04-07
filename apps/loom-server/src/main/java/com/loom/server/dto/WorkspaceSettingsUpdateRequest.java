package com.loom.server.dto;

import com.loom.server.model.CommandId;
import com.loom.server.model.SkillId;
import java.util.List;

public record WorkspaceSettingsUpdateRequest(
        String workspaceName,
        String language,
        String density,
        String defaultProjectId,
        String defaultLandingView,
        Boolean inspectorDefaultOpen,
        ModelSettingsUpdateRequest model,
        VaultSettingsUpdateRequest vault,
        WorkspaceNodeSettingsUpdateRequest nodes,
        AppearanceSettingsUpdateRequest appearance,
        List<CommandId> enabledCommands,
        List<SkillId> enabledSkills
) {
}
