package com.loom.server.repository;

import com.loom.server.model.AuditLogEntry;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class AuditLogRepository extends JdbcRepositorySupport {

    public AuditLogRepository(JdbcClient jdbcClient, JdbcJsonSupport jsonSupport) {
        super(jdbcClient, jsonSupport);
    }

    public void save(AuditLogEntry entry) {
        jdbcClient.sql("""
                insert into audit_logs (
                    id, actor, source, target_type, target_id, action, payload_hash, result, created_at
                ) values (
                    :id, :actor, :source, :targetType, :targetId, :action, :payloadHash, :result, :createdAt
                )
                """)
                .param("id", entry.id())
                .param("actor", entry.actor())
                .param("source", entry.source())
                .param("targetType", entry.targetType())
                .param("targetId", entry.targetId())
                .param("action", entry.action())
                .param("payloadHash", entry.payloadHash())
                .param("result", entry.result())
                .param("createdAt", entry.createdAt())
                .update();
    }

    public List<AuditLogEntry> recent(int limit) {
        return jdbcClient.sql("select * from audit_logs order by created_at desc limit :limit")
                .param("limit", limit)
                .query(this::mapRow)
                .list();
    }

    private AuditLogEntry mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        return new AuditLogEntry(
                resultSet.getString("id"),
                resultSet.getString("actor"),
                resultSet.getString("source"),
                resultSet.getString("target_type"),
                resultSet.getString("target_id"),
                resultSet.getString("action"),
                resultSet.getString("payload_hash"),
                resultSet.getString("result"),
                instant(resultSet, "created_at")
        );
    }
}
