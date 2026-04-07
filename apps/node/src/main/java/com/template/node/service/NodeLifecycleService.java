package com.template.node.service;

import com.template.node.client.TemplateServerGateway;
import com.template.node.config.NodeProperties;
import com.template.node.dto.NodeHeartbeatProbeRequest;
import com.template.node.dto.NodeHeartbeatRequest;
import com.template.node.dto.NodeRegistrationRequest;
import com.template.node.dto.NodeRegistrationResponse;
import com.template.node.model.NodeSnapshot;
import com.template.node.state.NodeStateStore;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class NodeLifecycleService {

    private static final Logger logger = LoggerFactory.getLogger(NodeLifecycleService.class);
    private final TemplateServerGateway templateServerGateway;
    private final NodeSnapshotProvider nodeSnapshotProvider;
    private final NodeStateStore nodeStateStore;
    private final NodeProperties nodeProperties;

    public NodeLifecycleService(
            TemplateServerGateway templateServerGateway,
            NodeSnapshotProvider nodeSnapshotProvider,
            NodeStateStore nodeStateStore,
            NodeProperties nodeProperties
    ) {
        this.templateServerGateway = templateServerGateway;
        this.nodeSnapshotProvider = nodeSnapshotProvider;
        this.nodeStateStore = nodeStateStore;
        this.nodeProperties = nodeProperties;
    }

    @PostConstruct
    void initialize() {
        nodeStateStore.loadNodeId().ifPresent(nodeId -> logger.info("Loaded persisted node id {}", nodeId));
    }

    @Scheduled(
            initialDelayString = "${template.node.heartbeat-initial-delay-ms:5000}",
            fixedDelayString = "${template.node.heartbeat-interval-ms:30000}"
    )
    public void sendScheduledHeartbeat() {
        heartbeatOnce();
    }

    public void heartbeatOnce() {
        NodeSnapshot snapshot = nodeSnapshotProvider.captureSnapshot();
        String nodeId = ensureRegisteredNodeId();
        templateServerGateway.heartbeat(
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
        NodeRegistrationResponse response = templateServerGateway.register(new NodeRegistrationRequest(
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
