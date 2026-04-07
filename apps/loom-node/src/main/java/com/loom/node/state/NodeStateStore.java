package com.loom.node.state;

import com.loom.node.config.NodeProperties;
import com.loom.node.dto.NodeSnapshot;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeStateStore {

    private static final Logger log = LoggerFactory.getLogger(NodeStateStore.class);

    private final Path stateFile;
    private final AtomicReference<State> state;

    public NodeStateStore(NodeProperties properties) {
        Objects.requireNonNull(properties, "properties");
        this.stateFile = Path.of(properties.getStateDir(), "node-state.properties");
        this.state = new AtomicReference<>(load());
    }

    public void markRegistered(String nodeId) {
        update(current -> new State(nodeId, Instant.now(), current.lastHeartbeatAt(), null, current.lastSnapshot()));
    }

    public void markHeartbeat(NodeSnapshot snapshot) {
        update(current -> new State(current.nodeId(), current.lastRegisteredAt(), Instant.now(), null, snapshot));
    }

    public void markError(String error) {
        update(current -> new State(current.nodeId(), current.lastRegisteredAt(), current.lastHeartbeatAt(), error, current.lastSnapshot()));
    }

    public State snapshot() {
        return state.get();
    }

    public Path stateFile() {
        return stateFile;
    }

    private void update(UnaryOperator<State> updater) {
        State nextState = state.updateAndGet(updater);
        persist(nextState);
    }

    private State load() {
        if (!Files.exists(stateFile)) {
            return emptyState();
        }

        Properties props = new Properties();
        try (Reader reader = Files.newBufferedReader(stateFile)) {
            props.load(reader);
            return new State(
                    emptyToNull(props.getProperty("nodeId")),
                    parseInstant(props.getProperty("lastRegisteredAt")),
                    parseInstant(props.getProperty("lastHeartbeatAt")),
                    emptyToNull(props.getProperty("lastError")),
                    null
            );
        } catch (Exception ex) {
            log.warn("Failed to load node state from {}: {}", stateFile, ex.getMessage());
            return emptyState();
        }
    }

    private void persist(State current) {
        try {
            Path parent = stateFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Properties props = new Properties();
            put(props, "nodeId", current.nodeId());
            put(props, "lastRegisteredAt", formatInstant(current.lastRegisteredAt()));
            put(props, "lastHeartbeatAt", formatInstant(current.lastHeartbeatAt()));
            put(props, "lastError", current.lastError());

            Path tempFile = stateFile.resolveSibling(stateFile.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(tempFile)) {
                props.store(writer, "Loom node state");
            }

            try {
                Files.move(tempFile, stateFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveFailure) {
                Files.move(tempFile, stateFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception ex) {
            log.warn("Failed to persist node state to {}: {}", stateFile, ex.getMessage());
        }
    }

    private State emptyState() {
        return new State(null, null, null, null, null);
    }

    private void put(Properties props, String key, String value) {
        if (value != null && !value.isBlank()) {
            props.setProperty(key, value);
        }
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private String formatInstant(Instant value) {
        return value == null ? null : value.toString();
    }

    public record State(
            String nodeId,
            Instant lastRegisteredAt,
            Instant lastHeartbeatAt,
            String lastError,
            NodeSnapshot lastSnapshot
    ) {
    }
}
