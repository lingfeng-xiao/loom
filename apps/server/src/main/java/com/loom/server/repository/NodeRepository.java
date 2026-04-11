package com.loom.server.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loom.server.dto.NodeHeartbeatProbeRequest;
import com.loom.server.dto.NodeHeartbeatRequest;
import com.loom.server.dto.NodeRegistrationRequest;
import com.loom.server.model.NodeRecord;
import com.loom.server.model.ProbeRecord;
import com.loom.server.model.ProbeStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class NodeRepository {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};
    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;

    public NodeRepository(JdbcClient jdbcClient, ObjectMapper objectMapper) {
        this.jdbcClient = jdbcClient;
        this.objectMapper = objectMapper;
    }

    public List<NodeRecord> findAll(int heartbeatTimeoutSeconds) {
        return jdbcClient.sql("""
                        select node_id, node_name, node_type, host, status, tags_json, capabilities_json,
                               last_heartbeat, created_at, updated_at
                        from loom_nodes
                        order by node_name asc
                        """)
                .query((rs, rowNum) -> mapNode(rs, heartbeatTimeoutSeconds))
                .list();
    }

    public Optional<NodeRecord> findById(String nodeId, int heartbeatTimeoutSeconds) {
        return jdbcClient.sql("""
                        select node_id, node_name, node_type, host, status, tags_json, capabilities_json,
                               last_heartbeat, created_at, updated_at
                        from loom_nodes
                        where node_id = :nodeId
                        """)
                .param("nodeId", nodeId)
                .query((rs, rowNum) -> mapNode(rs, heartbeatTimeoutSeconds))
                .optional();
    }

    public NodeRecord register(NodeRegistrationRequest request, int heartbeatTimeoutSeconds) {
        Instant now = Instant.now();
        String nodeId = "node-" + UUID.randomUUID().toString().substring(0, 8);
        jdbcClient.sql("""
                        insert into loom_nodes (
                            node_id, node_name, node_type, host, status, tags_json, capabilities_json,
                            last_heartbeat, created_at, updated_at
                        ) values (
                            :nodeId, :nodeName, :nodeType, :host, :status, :tagsJson, :capabilitiesJson,
                            :lastHeartbeat, :createdAt, :updatedAt
                        )
                        """)
                .param("nodeId", nodeId)
                .param("nodeName", request.name())
                .param("nodeType", request.type())
                .param("host", request.host())
                .param("status", ProbeStatus.unknown.name())
                .param("tagsJson", writeJson(request.tags()))
                .param("capabilitiesJson", writeJson(request.capabilities() == null ? List.of() : request.capabilities()))
                .param("lastHeartbeat", null)
                .param("createdAt", Timestamp.from(now))
                .param("updatedAt", Timestamp.from(now))
                .update();
        return findById(nodeId, heartbeatTimeoutSeconds).orElseThrow();
    }

    public NodeRecord heartbeat(String nodeId, NodeHeartbeatRequest request, int heartbeatTimeoutSeconds) {
        Instant now = Instant.now();
        int updated = jdbcClient.sql("""
                        update loom_nodes
                        set status = :status,
                            last_heartbeat = :lastHeartbeat,
                            updated_at = :updatedAt
                        where node_id = :nodeId
                        """)
                .param("status", request.status().name())
                .param("lastHeartbeat", Timestamp.from(now))
                .param("updatedAt", Timestamp.from(now))
                .param("nodeId", nodeId)
                .update();

        if (updated == 0) {
            throw new IllegalArgumentException("Unknown node id: " + nodeId);
        }

        jdbcClient.sql("delete from loom_node_probes where node_id = :nodeId")
                .param("nodeId", nodeId)
                .update();

        if (request.probes() != null) {
            for (NodeHeartbeatProbeRequest probe : request.probes()) {
                jdbcClient.sql("""
                                insert into loom_node_probes (
                                    node_id, probe_name, probe_kind, target, status, detail, recorded_at
                                ) values (
                                    :nodeId, :probeName, :probeKind, :target, :status, :detail, :recordedAt
                                )
                                """)
                        .param("nodeId", nodeId)
                        .param("probeName", probe.name())
                        .param("probeKind", probe.kind())
                        .param("target", probe.target())
                        .param("status", probe.status().name())
                        .param("detail", probe.detail())
                        .param("recordedAt", Timestamp.from(now))
                        .update();
            }
        }

        return findById(nodeId, heartbeatTimeoutSeconds).orElseThrow();
    }

    private NodeRecord mapNode(ResultSet rs, int heartbeatTimeoutSeconds) throws SQLException {
        Instant lastHeartbeat = rs.getTimestamp("last_heartbeat") == null
                ? null
                : rs.getTimestamp("last_heartbeat").toInstant();
        ProbeStatus status = ProbeStatus.valueOf(rs.getString("status"));
        if (lastHeartbeat != null && Duration.between(lastHeartbeat, Instant.now()).getSeconds() > heartbeatTimeoutSeconds) {
            status = ProbeStatus.down;
        }
        return new NodeRecord(
                rs.getString("node_id"),
                rs.getString("node_name"),
                rs.getString("node_type"),
                rs.getString("host"),
                status,
                readJsonList(rs.getString("tags_json")),
                readJsonList(rs.getString("capabilities_json")),
                lastHeartbeat,
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                loadProbes(rs.getString("node_id"))
        );
    }

    private List<ProbeRecord> loadProbes(String nodeId) {
        return jdbcClient.sql("""
                        select probe_name, probe_kind, target, status, detail, recorded_at
                        from loom_node_probes
                        where node_id = :nodeId
                        order by probe_name asc
                        """)
                .param("nodeId", nodeId)
                .query((rs, rowNum) -> new ProbeRecord(
                        rs.getString("probe_name"),
                        rs.getString("probe_kind"),
                        rs.getString("target"),
                        ProbeStatus.valueOf(rs.getString("status")),
                        rs.getString("detail"),
                        rs.getTimestamp("recorded_at").toInstant()
                ))
                .list();
    }

    private String writeJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize list", exception);
        }
    }

    private List<String> readJsonList(String rawValue) {
        try {
            return objectMapper.readValue(rawValue, STRING_LIST);
        } catch (JsonProcessingException exception) {
            return Collections.emptyList();
        }
    }
}
