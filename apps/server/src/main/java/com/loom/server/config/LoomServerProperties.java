package com.loom.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "loom")
public class LoomServerProperties {

    private String appName;
    private String description;
    private String docsUrl;
    private String supportEmail;
    private final Node node = new Node();
    private final Release release = new Release();

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDocsUrl() {
        return docsUrl;
    }

    public void setDocsUrl(String docsUrl) {
        this.docsUrl = docsUrl;
    }

    public String getSupportEmail() {
        return supportEmail;
    }

    public void setSupportEmail(String supportEmail) {
        this.supportEmail = supportEmail;
    }

    public Node getNode() {
        return node;
    }

    public Release getRelease() {
        return release;
    }

    public static class Node {
        private int heartbeatTimeoutSeconds = 90;
        private String serverToken = "change-me";

        public int getHeartbeatTimeoutSeconds() {
            return heartbeatTimeoutSeconds;
        }

        public void setHeartbeatTimeoutSeconds(int heartbeatTimeoutSeconds) {
            this.heartbeatTimeoutSeconds = heartbeatTimeoutSeconds;
        }

        public String getServerToken() {
            return serverToken;
        }

        public void setServerToken(String serverToken) {
            this.serverToken = serverToken;
        }
    }

    public static class Release {
        private String installRoot = "/opt/loom";
        private String systemdUnit = "loom.service";
        private String registry = "ghcr.io/example";

        public String getInstallRoot() {
            return installRoot;
        }

        public void setInstallRoot(String installRoot) {
            this.installRoot = installRoot;
        }

        public String getSystemdUnit() {
            return systemdUnit;
        }

        public void setSystemdUnit(String systemdUnit) {
            this.systemdUnit = systemdUnit;
        }

        public String getRegistry() {
            return registry;
        }

        public void setRegistry(String registry) {
            this.registry = registry;
        }
    }
}
