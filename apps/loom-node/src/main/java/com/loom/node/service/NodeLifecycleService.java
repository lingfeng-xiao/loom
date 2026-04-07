package com.loom.node.service;

import com.loom.node.client.NodeServerClient;
import com.loom.node.config.NodeProperties;
import com.loom.node.dto.NodeHeartbeatRequest;
import com.loom.node.dto.NodeRegistrationRequest;
import com.loom.node.dto.NodeRegistrationResponse;
import com.loom.node.state.NodeStateStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class NodeLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(NodeLifecycleService.class);

    private final NodeServerClient nodeServerClient;
    private final NodeSnapshotService nodeSnapshotService;
    private final NodeStateStore nodeStateStore;
    private final NodeProperties properties;

    public NodeLifecycleService(
            NodeServerClient nodeServerClient,
            NodeSnapshotService nodeSnapshotService,
            NodeStateStore nodeStateStore,
            NodeProperties properties
    ) {
        this.nodeServerClient = nodeServerClient;
        this.nodeSnapshotService = nodeSnapshotService;
        this.nodeStateStore = nodeStateStore;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        registerIfNeeded();
    }

    @Scheduled(fixedDelayString = "${loom.node.heartbeat-interval-ms:30000}", initialDelayString = "${loom.node.heartbeat-initial-delay-ms:5000}")
    public void heartbeat() {
        String nodeId = nodeStateStore.snapshot().nodeId();
        if (nodeId == null || nodeId.isBlank()) {
            registerIfNeeded();
            nodeId = nodeStateStore.snapshot().nodeId();
        }
        if (nodeId == null || nodeId.isBlank()) {
            return;
        }

        try {
            var snapshot = nodeSnapshotService.collect();
            var state = nodeStateStore.snapshot();
            nodeServerClient.heartbeat(
                    properties,
                    nodeId,
                    new NodeHeartbeatRequest(nodeId, properties.getName(), snapshot, Instant.now(), state.lastError())
            );
            nodeStateStore.markHeartbeat(snapshot);
            log.info("Sent heartbeat for node {}", nodeId);
        } catch (Exception ex) {
            nodeStateStore.markError(ex.getMessage());
            log.warn("Heartbeat failed: {}", ex.getMessage());
        }
    }

    private synchronized void registerIfNeeded() {
        if (nodeStateStore.snapshot().nodeId() != null) {
            return;
        }

        try {
            var snapshot = nodeSnapshotService.collect();
            List<String> capabilities = properties.getServiceProbes() == null || properties.getServiceProbes().isEmpty()
                    ? properties.configuredServiceNames()
                    : properties.getServiceProbes().stream().map(probe -> probe.getName() + ":" + probe.getKind()).toList();
            NodeRegistrationResponse response = nodeServerClient.register(
                    properties,
                    new NodeRegistrationRequest(
                            properties.getName(),
                            properties.getType(),
                            properties.getHost(),
                            new ArrayList<>(properties.getTags()),
                            capabilities,
                            properties.configuredServiceNames()
                    )
            );
            String nodeId = response.nodeId() != null ? response.nodeId() : properties.getName();
            nodeStateStore.markRegistered(nodeId);
            nodeStateStore.markHeartbeat(snapshot);
            log.info("Registered node {} as {}", properties.getName(), nodeId);
        } catch (Exception ex) {
            nodeStateStore.markError(ex.getMessage());
            log.warn("Node registration failed: {}", ex.getMessage());
        }
    }
}
