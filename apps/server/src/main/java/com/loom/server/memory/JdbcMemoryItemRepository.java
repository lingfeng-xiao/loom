package com.loom.server.memory;

import com.loom.server.jdbc.JdbcSchemaSupport;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcMemoryItemRepository implements MemoryItemRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcMemoryItemRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    void ensureSchema() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS loom_memory_items (
                    id VARCHAR(80) PRIMARY KEY,
                    scope VARCHAR(40) NOT NULL,
                    project_id VARCHAR(80),
                    conversation_id VARCHAR(80),
                    content TEXT NOT NULL,
                    source VARCHAR(40) NOT NULL,
                    updated_at VARCHAR(40) NOT NULL
                )
                """);
        JdbcSchemaSupport.ensureIndex(
                jdbcTemplate,
                "loom_memory_items",
                "idx_loom_memory_items_project",
                "project_id, conversation_id, scope, updated_at"
        );
    }

    @Override
    public MemoryItemRecord save(MemoryItemRecord record) {
        if (findById(record.id()).isPresent()) {
            jdbcTemplate.update("""
                    UPDATE loom_memory_items
                    SET scope = ?, project_id = ?, conversation_id = ?, content = ?, source = ?, updated_at = ?
                    WHERE id = ?
                    """, record.scope(), record.projectId(), record.conversationId(), record.content(), record.source(), record.updatedAt(), record.id());
            return record;
        }
        jdbcTemplate.update("""
                INSERT INTO loom_memory_items (id, scope, project_id, conversation_id, content, source, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, record.id(), record.scope(), record.projectId(), record.conversationId(), record.content(), record.source(), record.updatedAt());
        return record;
    }

    @Override
    public Optional<MemoryItemRecord> findById(String id) {
        List<MemoryItemRecord> items = jdbcTemplate.query("""
                        SELECT id, scope, project_id, conversation_id, content, source, updated_at
                        FROM loom_memory_items
                        WHERE id = ?
                        LIMIT 1
                        """,
                this::mapRow,
                id);
        return items.stream().findFirst();
    }

    @Override
    public List<MemoryItemRecord> listByProject(String projectId) {
        return jdbcTemplate.query("""
                        SELECT id, scope, project_id, conversation_id, content, source, updated_at
                        FROM loom_memory_items
                        WHERE project_id = ? OR project_id IS NULL
                        ORDER BY updated_at DESC, id DESC
                        """,
                this::mapRow,
                projectId);
    }

    @Override
    public void deleteById(String id) {
        jdbcTemplate.update("DELETE FROM loom_memory_items WHERE id = ?", id);
    }

    private MemoryItemRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new MemoryItemRecord(
                rs.getString("id"),
                rs.getString("scope"),
                rs.getString("project_id"),
                rs.getString("conversation_id"),
                rs.getString("content"),
                rs.getString("source"),
                rs.getString("updated_at")
        );
    }
}

