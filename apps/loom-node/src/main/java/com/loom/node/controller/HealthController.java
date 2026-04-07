package com.loom.node.controller;

import com.loom.node.config.NodeProperties;
import com.loom.node.dto.NodeRuntimeStatus;
import com.loom.node.dto.NodeServiceStatus;
import com.loom.node.state.NodeStateStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final NodeStateStore stateStore;
    private final NodeProperties properties;

    public HealthController(NodeStateStore stateStore, NodeProperties properties) {
        this.stateStore = stateStore;
        this.properties = properties;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        var state = stateStore.snapshot();
        boolean degraded = state.lastError() != null && !state.lastError().isBlank();
        return Map.of(
                "status", degraded ? "degraded" : "ok",
                "service", "loom-node",
                "nodeId", state.nodeId() == null ? "" : state.nodeId(),
                "lastError", state.lastError() == null ? "" : state.lastError()
        );
    }

    @GetMapping("/node/runtime")
    public NodeRuntimeStatus runtime() {
        var state = stateStore.snapshot();
        List<NodeServiceStatus> services = state.lastSnapshot() == null ? List.of() : state.lastSnapshot().services();
        return new NodeRuntimeStatus(
                state.nodeId(),
                properties.getName(),
                properties.getType(),
                properties.getServerBaseUrl(),
                state.nodeId() != null && !state.nodeId().isBlank(),
                state.lastRegisteredAt(),
                state.lastHeartbeatAt(),
                state.lastError(),
                stateStore.stateFile().toString(),
                state.lastSnapshot(),
                services
        );
    }
}
