package com.template.node.service;

import com.template.node.client.TemplateServerGateway;
import com.template.node.config.NodeProperties;
import com.template.node.dto.NodeHeartbeatRequest;
import com.template.node.dto.NodeHeartbeatResponse;
import com.template.node.dto.NodeRegistrationRequest;
import com.template.node.dto.NodeRegistrationResponse;
import com.template.node.model.NodeProbeResult;
import com.template.node.model.NodeSnapshot;
import com.template.node.model.ProbeStatus;
import com.template.node.state.NodeStateStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NodeLifecycleServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void registersOnceAndThenSendsHeartbeats() {
        NodeProperties properties = new NodeProperties();
        properties.setStateDir(tempDir);
        properties.setName("template-node");
        properties.setType("server");
        properties.setHost("localhost");
        properties.setTags(List.of("local"));
        properties.setCapabilities(List.of("heartbeat"));

        RecordingGateway gateway = new RecordingGateway();
        NodeSnapshot snapshot = new NodeSnapshot(
                "localhost",
                ProbeStatus.up,
                List.of(new NodeProbeResult("template-server", "http", "http://template-server:8080/api/health", ProbeStatus.up, "HTTP 200", Instant.now())),
                Instant.now()
        );
        NodeLifecycleService service = new NodeLifecycleService(
                gateway,
                () -> snapshot,
                new NodeStateStore(properties),
                properties
        );

        service.heartbeatOnce();
        service.heartbeatOnce();

        assertThat(gateway.registrationRequests).hasSize(1);
        assertThat(gateway.heartbeatRequests).hasSize(2);
        assertThat(gateway.heartbeatNodeIds).containsOnly("node-template");
    }

    private static class RecordingGateway implements TemplateServerGateway {
        private final List<NodeRegistrationRequest> registrationRequests = new ArrayList<>();
        private final List<NodeHeartbeatRequest> heartbeatRequests = new ArrayList<>();
        private final List<String> heartbeatNodeIds = new ArrayList<>();

        @Override
        public NodeRegistrationResponse register(NodeRegistrationRequest request) {
            registrationRequests.add(request);
            return new NodeRegistrationResponse("node-template", Instant.now());
        }

        @Override
        public NodeHeartbeatResponse heartbeat(String nodeId, NodeHeartbeatRequest request) {
            heartbeatNodeIds.add(nodeId);
            heartbeatRequests.add(request);
            return new NodeHeartbeatResponse(nodeId, Instant.now());
        }
    }
}
