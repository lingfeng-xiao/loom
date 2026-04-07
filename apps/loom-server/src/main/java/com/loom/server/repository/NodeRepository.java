package com.loom.server.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.loom.server.model.Node;
import com.loom.server.model.NodeSnapshot;
import com.loom.server.model.NodeStatus;
import com.loom.server.model.NodeType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class NodeRepository extends JdbcRepositorySupport {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    public NodeRepository(JdbcClient jdbcClient, JdbcJsonSupport jsonSupport) {
        super(jdbcClient, jsonSupport);
    }

    public List<Node> findAll() {
        return jdbcClient.sql("select * from nodes order by updated_at desc")
                .query(this::mapRow)
                .list();
    }

    public Optional<Node> findById(String id) {
        return jdbcClient.sql("select * from nodes where id = :id")
                .param("id", id)
                .query(this::mapRow)
                .optional();
    }

    public Optional<Node> findByNameAndHost(String name, String host) {
        return jdbcClient.sql("select * from nodes where name = :name and host = :host")
                .param("name", name)
                .param("host", host)
                .query(this::mapRow)
                .optional();
    }

    public void save(Node node) {
        jdbcClient.sql("""
                insert into nodes (
                    id, name, type, host, tags, status, last_heartbeat, snapshot, capabilities, last_error, created_at, updated_at
                ) values (
                    :id, :name, :type, :host, :tags, :status, :lastHeartbeat, :snapshot, :capabilities, :lastError, :createdAt, :updatedAt
                )
                on duplicate key update
                    name = values(name),
                    type = values(type),
                    host = values(host),
                    tags = values(tags),
                    status = values(status),
                    last_heartbeat = values(last_heartbeat),
                    snapshot = values(snapshot),
                    capabilities = values(capabilities),
                    last_error = values(last_error),
                    updated_at = values(updated_at)
                """)
                .param("id", node.id())
                .param("name", node.name())
                .param("type", node.type().value())
                .param("host", node.host())
                .param("tags", jsonSupport.write(node.tags()))
                .param("status", node.status().value())
                .param("lastHeartbeat", node.lastHeartbeat())
                .param("snapshot", node.snapshot() == null ? null : jsonSupport.write(node.snapshot()))
                .param("capabilities", jsonSupport.write(node.capabilities()))
                .param("lastError", node.lastError())
                .param("createdAt", node.createdAt())
                .param("updatedAt", node.updatedAt())
                .update();
    }

    private Node mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        return new Node(
                resultSet.getString("id"),
                resultSet.getString("name"),
                NodeType.fromValue(resultSet.getString("type")),
                resultSet.getString("host"),
                jsonSupport.read(resultSet.getString("tags"), STRING_LIST, List::of),
                NodeStatus.fromValue(resultSet.getString("status")),
                instant(resultSet, "last_heartbeat"),
                jsonSupport.read(resultSet.getString("snapshot"), NodeSnapshot.class, () -> null),
                jsonSupport.read(resultSet.getString("capabilities"), STRING_LIST, List::of),
                resultSet.getString("last_error"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at")
        );
    }
}
