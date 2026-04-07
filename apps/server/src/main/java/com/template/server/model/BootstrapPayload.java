package com.template.server.model;

import java.util.List;

public record BootstrapPayload(
        String appName,
        String description,
        WorkspaceSettings workspaceSettings,
        ReleaseOverview releaseOverview,
        List<SetupTask> setupTasks,
        List<ExtensionPoint> extensionPoints
) {
}
