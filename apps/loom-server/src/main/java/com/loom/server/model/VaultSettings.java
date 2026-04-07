package com.loom.server.model;

public record VaultSettings(
        String serverVaultRoot,
        String localVaultRoot,
        String assetPathTemplate,
        String writeTarget
) {
}
