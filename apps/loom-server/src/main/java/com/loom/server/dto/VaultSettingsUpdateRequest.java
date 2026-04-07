package com.loom.server.dto;

public record VaultSettingsUpdateRequest(
        String serverVaultRoot,
        String localVaultRoot,
        String assetPathTemplate,
        String writeTarget
) {
}
