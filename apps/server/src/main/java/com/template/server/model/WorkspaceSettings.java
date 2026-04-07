package com.template.server.model;

import java.time.Instant;

public record WorkspaceSettings(
        String workspaceName,
        String supportEmail,
        String docsUrl,
        int defaultRefreshIntervalSeconds,
        Instant updatedAt
) {
}
