package com.loom.node.state;

import com.loom.node.config.NodeProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class NodeStateStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void storesAndLoadsNodeId() {
        NodeProperties properties = new NodeProperties();
        properties.setStateDir(tempDir);
        NodeStateStore store = new NodeStateStore(properties);

        store.saveNodeId("node-1234");

        assertThat(store.loadNodeId()).contains("node-1234");
    }
}
