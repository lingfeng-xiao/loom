package com.loom.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "loom.llm")
public class LoomLlmProperties {

    private final Minimax minimax = new Minimax();

    public Minimax getMinimax() {
        return minimax;
    }

    public static class Minimax {
        private String apiBaseUrl = "https://api.minimaxi.com/v1";
        private String apiKey = "";
        private String modelId = "MiniMax-M2.7";
        private String displayName = "MiniMax M2.7";
        private String systemPrompt = "You are Loom, a careful AI teammate that explains decisions clearly and keeps execution visible.";
        private double temperature = 1.0d;
        private Integer maxTokens = 1024;
        private int timeoutMs = 60000;

        public String getApiBaseUrl() {
            return apiBaseUrl;
        }

        public void setApiBaseUrl(String apiBaseUrl) {
            this.apiBaseUrl = apiBaseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModelId() {
            return modelId;
        }

        public void setModelId(String modelId) {
            this.modelId = modelId;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getSystemPrompt() {
            return systemPrompt;
        }

        public void setSystemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public Integer getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }
}
