package com.loom.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "loom")
public class LoomProperties {

    private final Storage storage = new Storage();
    private final Bootstrap bootstrap = new Bootstrap();
    private final Nodes nodes = new Nodes();
    private final AI ai = new AI();

    public Storage getStorage() {
        return storage;
    }

    public Bootstrap getBootstrap() {
        return bootstrap;
    }

    public Nodes getNodes() {
        return nodes;
    }

    public AI getAi() {
        return ai;
    }

    public static class Storage {
        private String serverVaultRoot = "./data/obsidian-server";
        private String localVaultRoot = "./data/obsidian-local";

        public String getServerVaultRoot() {
            return serverVaultRoot;
        }

        public void setServerVaultRoot(String serverVaultRoot) {
            this.serverVaultRoot = serverVaultRoot;
        }

        public String getLocalVaultRoot() {
            return localVaultRoot;
        }

        public void setLocalVaultRoot(String localVaultRoot) {
            this.localVaultRoot = localVaultRoot;
        }
    }

    public static class Bootstrap {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Nodes {
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

    public static class AI {
        private String providerLabel = "OpenAI Compatible";
        private String baseUrl = "http://127.0.0.1:11434/v1";
        private String model = "gpt-4.1-mini";
        private double temperature = 0.2;
        private String apiKey = "";
        private String systemPrompt = """
                You are Loom, the assistant inside a project-first AI workspace.
                Reply in the user's language when possible, keep answers concise and actionable,
                and use Markdown when it improves readability.
                """;

        public String getProviderLabel() {
            return providerLabel;
        }

        public void setProviderLabel(String providerLabel) {
            this.providerLabel = providerLabel;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getSystemPrompt() {
            return systemPrompt;
        }

        public void setSystemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
        }
    }
}
