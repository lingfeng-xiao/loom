package com.loom.server.model;

public record WorkspaceNodeSettings(
        int heartbeatTimeoutSeconds,
        boolean inspectorShowOffline,
        String centerNodeLabel
) {
}
