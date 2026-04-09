package com.loom.server.context;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcContextSnapshotRepository implements ContextSnapshotRepository {
    private static final String TABLE = "loom_context_snapshots";

    private final JdbcTemplate jdbcTemplate;

    public JdbcContextSnapshotRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    void ensureSchema() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS loom_context_snapshots (
                    id VARCHAR(80) PRIMARY KEY,
                    project_id VARCHAR(80) NOT NULL,
                    conversation_id VARCHAR(80) NOT NULL,
                    kind VARCHAR(40) NOT NULL,
                    content TEXT NOT NULL,
                    updated_at VARCHAR(40) NOT NULL
                )
                """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_loom_context_snapshots_conversation ON loom_context_snapshots(project_id, conversation_id, updated_at)");
    }

    @Override
    public ContextSnapshotRecord save(ContextSnapshotRecord record) {
        jdbcTemplate.update("""
                INSERT INTO loom_context_snapshots (id, project_id, conversation_id, kind, content, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, record.id(), record.projectId(), record.conversationId(), record.kind(), record.content(), record.updatedAt());
        return record;
    }

    @Override
    public List<ContextSnapshotRecord> listByConversation(String projectId, String conversationId) {
        return jdbcTemplate.query("""
                        SELECT id, project_id, conversation_id, kind, content, updated_at
                        FROM loom_context_snapshots
                        WHERE project_id = ? AND conversation_id = ?
                        ORDER BY updated_at DESC, id DESC
                        """,
                this::mapRow,
                projectId,
                conversationId);
    }

    @Override
    public List<ContextSnapshotRecord> listByProject(String projectId) {
        return jdbcTemplate.query("""
                        SELECT id, project_id, conversation_id, kind, content, updated_at
                        FROM loom_context_snapshots
                        WHERE project_id = ?
                        ORDER BY updated_at DESC, id DESC
                        """,
                this::mapRow,
                projectId);
    }

    @Override
    public Optional<ContextSnapshotRecord> findLatestByConversation(String projectId, String conversationId) {
        List<ContextSnapshotRecord> snapshots = jdbcTemplate.query("""
                        SELECT id, project_id, conversation_id, kind, content, updated_at
                        FROM loom_context_snapshots
                        WHERE project_id = ? AND conversation_id = ?
                        ORDER BY updated_at DESC, id DESC
                        LIMIT 1
                        """,
                this::mapRow,
                projectId,
                conversationId);
        return snapshots.stream().findFirst();
    }

    private ContextSnapshotRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new ContextSnapshotRecord(
                rs.getString("id"),
                rs.getString("project_id"),
                rs.getString("conversation_id"),
                rs.getString("kind"),
                rs.getString("content"),
                rs.getString("updated_at")
        );
    }
}
