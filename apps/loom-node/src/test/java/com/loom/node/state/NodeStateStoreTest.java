package com.loom.node.state;

import com.loom.node.config.NodeProperties;
import com.loom.node.dto.NodeSnapshot;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class NodeStateStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void persistsNodeIdAndRuntimeStateAcrossRestarts() {
        NodeProperties properties = baseProperties();
        properties.setStateDir(tempDir.toString());

        NodeStateStore first = new NodeStateStore(properties);
        first.markRegistered("node-123");
        first.markHeartbeat(snapshot());
        first.markError("network down");

        NodeStateStore restored = new NodeStateStore(properties);
        assertThat(restored.snapshot().nodeId()).isEqualTo("node-123");
        assertThat(restored.snapshot().lastRegisteredAt()).isNotNull();
        assertThat(restored.snapshot().lastHeartbeatAt()).isNotNull();
        assertThat(restored.snapshot().lastError()).isEqualTo("network down");
        assertThat(restored.stateFile()).exists();
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

    private NodeSnapshot snapshot() {
        return new NodeSnapshot(
                "localhost",
                "Linux",
                12.5,
                8.4,
                8_000L,
                2_500L,
                5_500L,
                18.0,
                List.of(),
                Instant.now()
        );
    }
}
