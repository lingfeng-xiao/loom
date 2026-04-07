package com.loom.node.service;

import com.loom.node.client.NodeServerClient;
import com.loom.node.config.NodeProperties;
import com.loom.node.dto.NodeHeartbeatRequest;
import com.loom.node.dto.NodeHeartbeatResponse;
import com.loom.node.dto.NodeRegistrationRequest;
import com.loom.node.dto.NodeRegistrationResponse;
import com.loom.node.state.NodeStateStore;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class NodeLifecycleServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void registersOnceAndReusesPersistedNodeIdAfterRestart() {
        NodeProperties properties = baseProperties();
        properties.setStateDir(tempDir.toString());
        properties.setServiceProbes(List.of());

        RecordingNodeServerClient firstClient = new RecordingNodeServerClient("node-abc");
        NodeStateStore firstStore = new NodeStateStore(properties);
        NodeLifecycleService firstLifecycle = new NodeLifecycleService(
                firstClient,
                new NodeSnapshotService(properties, new NodeProbeService(properties)),
                firstStore,
                properties
        );

        firstLifecycle.onApplicationReady();
        firstLifecycle.heartbeat();

        assertThat(firstClient.registerCount).isEqualTo(1);
        assertThat(firstClient.heartbeatCount).isEqualTo(1);
        assertThat(firstStore.snapshot().nodeId()).isEqualTo("node-abc");

        RecordingNodeServerClient secondClient = new RecordingNodeServerClient("node-xyz");
        NodeStateStore restoredStore = new NodeStateStore(properties);
        NodeLifecycleService secondLifecycle = new NodeLifecycleService(
                secondClient,
                new NodeSnapshotService(properties, new NodeProbeService(properties)),
                restoredStore,
                properties
        );

        secondLifecycle.onApplicationReady();
        secondLifecycle.heartbeat();

        assertThat(secondClient.registerCount).isZero();
        assertThat(secondClient.heartbeatCount).isEqualTo(1);
        assertThat(restoredStore.snapshot().nodeId()).isEqualTo("node-abc");
        assertThat(secondClient.lastHeartbeatRequest.lastError()).isNull();
    }

    private NodeProperties baseProperties() {
        NodeProperties properties = new NodeProperties();
        properties.setName("loom-node");
        properties.setType("server");
        properties.setHost("localhost");
        properties.setServerBaseUrl("http://localhost:8080");
        properties.setServerToken("token");
        properties.setServiceNames(List.of("loom-server"));
        return properties;
    }

    private static class RecordingNodeServerClient extends NodeServerClient {

        private final String nodeId;
        private int registerCount;
        private int heartbeatCount;
        private NodeHeartbeatRequest lastHeartbeatRequest;

        RecordingNodeServerClient(String nodeId) {
            super(new RestTemplate());
            this.nodeId = nodeId;
        }

        @Override
        public NodeRegistrationResponse register(NodeProperties properties, NodeRegistrationRequest request) {
            registerCount++;
            return new NodeRegistrationResponse(nodeId, "registered", "ok");
        }

        @Override
        public NodeHeartbeatResponse heartbeat(NodeProperties properties, String nodeId, NodeHeartbeatRequest request) {
            heartbeatCount++;
            lastHeartbeatRequest = request;
            return new NodeHeartbeatResponse(nodeId, "acknowledged", "ok");
        }
    }
}
