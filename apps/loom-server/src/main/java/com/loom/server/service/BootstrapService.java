package com.loom.server.service;

import com.loom.server.model.CommandId;
import com.loom.server.support.Responses.BootstrapResponse;
import org.springframework.stereotype.Service;

@Service
public class BootstrapService {

    private final ProjectService projectService;
    private final SkillService skillService;
    private final NodeService nodeService;
    private final WorkspaceSettingsService workspaceSettingsService;

    public BootstrapService(
            ProjectService projectService,
            SkillService skillService,
            NodeService nodeService,
            WorkspaceSettingsService workspaceSettingsService
    ) {
        this.projectService = projectService;
        this.skillService = skillService;
        this.nodeService = nodeService;
        this.workspaceSettingsService = workspaceSettingsService;
    }

    public BootstrapResponse load() {
        var settings = workspaceSettingsService.getSettings();
        return new BootstrapResponse(
                projectService.listProjects(),
                skillService.listSkills(),
                nodeService.listNodes(),
                java.util.List.of(CommandId.values()),
                settings.defaultProjectId(),
                settings
        );
    }
}
