package com.loom.node.service;

import com.loom.node.client.LoomServerGateway;
import com.loom.node.config.NodeProperties;
import com.loom.node.dto.NodeHeartbeatProbeRequest;
import com.loom.node.dto.NodeHeartbeatRequest;
import com.loom.node.dto.NodeRegistrationRequest;
import com.loom.node.dto.NodeRegistrationResponse;
import com.loom.node.model.NodeSnapshot;
import com.loom.node.state.NodeStateStore;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class NodeLifecycleService {

    private static final Logger logger = LoggerFactory.getLogger(NodeLifecycleService.class);
    private final LoomServerGateway loomServerGateway;
    private final NodeSnapshotProvider nodeSnapshotProvider;
    private final NodeStateStore nodeStateStore;
    private final NodeProperties nodeProperties;

    public NodeLifecycleService(
            LoomServerGateway loomServerGateway,
            NodeSnapshotProvider nodeSnapshotProvider,
            NodeStateStore nodeStateStore,
            NodeProperties nodeProperties
    ) {
        this.loomServerGateway = loomServerGateway;
        this.nodeSnapshotProvider = nodeSnapshotProvider;
        this.nodeStateStore = nodeStateStore;
        this.nodeProperties = nodeProperties;
    }

    @PostConstruct
    void initialize() {
        nodeStateStore.loadNodeId().ifPresent(nodeId -> logger.info("Loaded persisted node id {}", nodeId));
    }

    @Scheduled(
            initialDelayString = "${loom.node.heartbeat-initial-delay-ms:5000}",
            fixedDelayString = "${loom.node.heartbeat-interval-ms:30000}"
    )
    public void sendScheduledHeartbeat() {
        heartbeatOnce();
    }

    public void heartbeatOnce() {
        NodeSnapshot snapshot = nodeSnapshotProvider.captureSnapshot();
        String nodeId = ensureRegisteredNodeId();
        loomServerGateway.heartbeat(
                nodeId,
                new NodeHeartbeatRequest(
                        snapshot.status(),
                        snapshot.probes().stream()
                                .map(probe -> new NodeHeartbeatProbeRequest(
                                        probe.name(),
                                        probe.kind(),
                                        probe.target(),
                                        probe.status(),
                                        probe.detail()
                                ))
                                .toList()
                )
        );
        logger.info("Sent heartbeat for node {}", nodeId);
    }

    private String ensureRegisteredNodeId() {
        Optional<String> existingNodeId = nodeStateStore.loadNodeId();
        if (existingNodeId.isPresent()) {
            return existingNodeId.get();
        }
        NodeRegistrationResponse response = loomServerGateway.register(new NodeRegistrationRequest(
                nodeProperties.getName(),
                nodeProperties.getType(),
                nodeProperties.getHost(),
                nodeProperties.getTags(),
                nodeProperties.getCapabilities()
        ));
        nodeStateStore.saveNodeId(response.nodeId());
        logger.info("Registered node {} as {}", nodeProperties.getName(), response.nodeId());
        return response.nodeId();
    }
}
