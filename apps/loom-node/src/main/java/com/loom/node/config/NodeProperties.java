package com.loom.node.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ConfigurationProperties(prefix = "loom.node")
@Validated
public class NodeProperties {

    @NotBlank
    private String name;

    @NotBlank
    private String type;

    @NotBlank
    private String host;

    @NotBlank
    private String serverBaseUrl;

    @NotBlank
    private String serverToken;

    @Min(1000)
    private long heartbeatIntervalMs = 30000;

    @Min(0)
    private long heartbeatInitialDelayMs = 5000;

    @NotBlank
    private String stateDir = "./data/node-state";

    private List<String> tags = new ArrayList<>();

    private List<String> serviceNames = new ArrayList<>();

    @jakarta.validation.Valid
    private List<ServiceProbe> serviceProbes = new ArrayList<>();

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

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getServiceNames() {
        return serviceNames;
    }

    public void setServiceNames(List<String> serviceNames) {
        this.serviceNames = serviceNames;
    }

    public String getStateDir() {
        return stateDir;
    }

    public void setStateDir(String stateDir) {
        this.stateDir = stateDir;
    }

    public List<ServiceProbe> getServiceProbes() {
        return serviceProbes;
    }

    public void setServiceProbes(List<ServiceProbe> serviceProbes) {
        this.serviceProbes = serviceProbes;
    }

    public List<String> configuredServiceNames() {
        if (serviceProbes != null && !serviceProbes.isEmpty()) {
            return serviceProbes.stream().map(ServiceProbe::getName).toList();
        }
        if (serviceNames == null || serviceNames.isEmpty()) {
            return Collections.emptyList();
        }
        return List.copyOf(serviceNames);
    }

    public static class ServiceProbe {
        @NotBlank
        private String name;

        @NotBlank
        private String kind = "http";

        @NotBlank
        private String target;

        @Min(100)
        private long timeoutMs = 3000;

        private Integer expectedStatus = 200;

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

        public long getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public Integer getExpectedStatus() {
            return expectedStatus;
        }

        public void setExpectedStatus(Integer expectedStatus) {
            this.expectedStatus = expectedStatus;
        }
    }
}
