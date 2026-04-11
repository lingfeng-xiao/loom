package com.loom.node.state;

import com.loom.node.config.NodeProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Component
public class NodeStateStore {

    private static final String NODE_ID_FILE = "node-id.txt";
    private final Path nodeIdPath;

    public NodeStateStore(NodeProperties nodeProperties) {
        this.nodeIdPath = nodeProperties.getStateDir().resolve(NODE_ID_FILE);
    }

    public Optional<String> loadNodeId() {
        if (!Files.exists(nodeIdPath)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readString(nodeIdPath, StandardCharsets.UTF_8).trim()).filter(value -> !value.isEmpty());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read node id", exception);
        }
    }

    public void saveNodeId(String nodeId) {
        try {
            Files.createDirectories(nodeIdPath.getParent());
            Files.writeString(nodeIdPath, nodeId, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to store node id", exception);
        }
    }
}
