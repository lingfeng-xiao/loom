package com.loom.server.dto;

public record WorkspaceNodeSettingsUpdateRequest(
        Integer heartbeatTimeoutSeconds,
        Boolean inspectorShowOffline,
        String centerNodeLabel
) {
}
