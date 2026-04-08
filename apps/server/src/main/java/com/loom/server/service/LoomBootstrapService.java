package com.loom.server.service;

import com.loom.server.config.LoomServerProperties;
import com.loom.server.model.BootstrapPayload;
import com.loom.server.workspace.WorkspaceStateService;
import org.springframework.stereotype.Service;

@Service
public class LoomBootstrapService {

    private final LoomServerProperties serverProperties;
    private final WorkspaceStateService workspaceStateService;

    public LoomBootstrapService(LoomServerProperties serverProperties, WorkspaceStateService workspaceStateService) {
        this.serverProperties = serverProperties;
        this.workspaceStateService = workspaceStateService;
    }

    public BootstrapPayload getBootstrap() {
        BootstrapPayload payload = workspaceStateService.buildBootstrapPayload();
        return new BootstrapPayload(
                coalesce(serverProperties.getAppName(), payload.appName()),
                coalesce(serverProperties.getDescription(), payload.description()),
                payload.project(),
                payload.pages(),
                payload.recentConversations(),
                payload.pinnedConversations(),
                payload.modes(),
                payload.activeMode(),
                payload.conversationTitle(),
                payload.conversationMeta(),
                payload.messages(),
                payload.composer(),
                payload.traceSummary(),
                payload.traceSteps(),
                payload.contextBlocks(),
                payload.capabilities(),
                payload.openClaw(),
                payload.settings()
        );
    }

    private String coalesce(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
