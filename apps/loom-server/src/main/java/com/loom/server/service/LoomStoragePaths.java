package com.loom.server.service;

import com.loom.server.config.LoomProperties;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

@Component
public class LoomStoragePaths {

    private final LoomProperties properties;

    public LoomStoragePaths(LoomProperties properties) {
        this.properties = properties;
    }

    public Path serverVaultRoot() {
        return Path.of(properties.getStorage().getServerVaultRoot()).toAbsolutePath().normalize();
    }

    public Path localVaultRoot() {
        return Path.of(properties.getStorage().getLocalVaultRoot()).toAbsolutePath().normalize();
    }
}
