package com.template.node.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "template.node")
public class NodeProperties {

    private String name = "template-node";
    private String type = "server";
    private String host = "localhost";
    private String serverBaseUrl = "http://localhost:8080";
    private String serverToken = "change-me";
    private long heartbeatIntervalMs = 30000;
    private long heartbeatInitialDelayMs = 5000;
    private Path stateDir = Path.of("./data/node-state");
    private List<String> tags = new ArrayList<>(List.of("local", "template"));
    private List<String> capabilities = new ArrayList<>(List.of("http-probe", "heartbeat"));
    private List<Probe> serviceProbes = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getServerBaseUrl() {
        return serverBaseUrl;
    }

    public void setServerBaseUrl(String serverBaseUrl) {
        this.serverBaseUrl = serverBaseUrl;
    }

    public String getServerToken() {
        return serverToken;
    }

    public void setServerToken(String serverToken) {
        this.serverToken = serverToken;
    }

    public long getHeartbeatIntervalMs() {
        return heartbeatIntervalMs;
    }

    public void setHeartbeatIntervalMs(long heartbeatIntervalMs) {
        this.heartbeatIntervalMs = heartbeatIntervalMs;
    }

    public long getHeartbeatInitialDelayMs() {
        return heartbeatInitialDelayMs;
    }

    public void setHeartbeatInitialDelayMs(long heartbeatInitialDelayMs) {
        this.heartbeatInitialDelayMs = heartbeatInitialDelayMs;
    }

    public Path getStateDir() {
        return stateDir;
    }

    public void setStateDir(Path stateDir) {
        this.stateDir = stateDir;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(List<String> capabilities) {
        this.capabilities = capabilities;
    }

    public List<Probe> getServiceProbes() {
        return serviceProbes;
    }

    public void setServiceProbes(List<Probe> serviceProbes) {
        this.serviceProbes = serviceProbes;
    }

    public static class Probe {
        private String name;
        private String kind = "http";
        private String target;
        private int timeoutMs = 3000;
        private int expectedStatus = 200;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getKind() {
            return kind;
        }

        public void setKind(String kind) {
            this.kind = kind;
        }

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public int getExpectedStatus() {
            return expectedStatus;
        }

        public void setExpectedStatus(int expectedStatus) {
            this.expectedStatus = expectedStatus;
        }
    }
}
