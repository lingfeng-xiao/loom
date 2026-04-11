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
public class JdbcMemorySuggestionRepository implements MemorySuggestionRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcMemorySuggestionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    void ensureSchema() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS loom_memory_suggestions (
                    id VARCHAR(80) PRIMARY KEY,
                    scope VARCHAR(40) NOT NULL,
                    project_id VARCHAR(80),
                    conversation_id VARCHAR(80),
                    content TEXT NOT NULL,
                    status VARCHAR(40) NOT NULL,
                    created_at VARCHAR(40) NOT NULL,
                    updated_at VARCHAR(40) NOT NULL
                )
                """);
        JdbcSchemaSupport.ensureIndex(
                jdbcTemplate,
                "loom_memory_suggestions",
                "idx_loom_memory_suggestions_project",
                "project_id, conversation_id, scope, status, created_at"
        );
    }

    @Override
    public MemorySuggestionRecord save(MemorySuggestionRecord record) {
        if (findById(record.id()).isPresent()) {
            jdbcTemplate.update("""
                    UPDATE loom_memory_suggestions
                    SET scope = ?, project_id = ?, conversation_id = ?, content = ?, status = ?, updated_at = ?
                    WHERE id = ?
                    """, record.scope(), record.projectId(), record.conversationId(), record.content(), record.status(), record.updatedAt(), record.id());
            return record;
        }
        jdbcTemplate.update("""
                INSERT INTO loom_memory_suggestions (id, scope, project_id, conversation_id, content, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, record.id(), record.scope(), record.projectId(), record.conversationId(), record.content(), record.status(), record.createdAt(), record.updatedAt());
        return record;
    }

    @Override
    public Optional<MemorySuggestionRecord> findById(String id) {
        List<MemorySuggestionRecord> suggestions = jdbcTemplate.query("""
                        SELECT id, scope, project_id, conversation_id, content, status, created_at, updated_at
                        FROM loom_memory_suggestions
                        WHERE id = ?
                        LIMIT 1
                        """,
                this::mapRow,
                id);
        return suggestions.stream().findFirst();
    }

    @Override
    public List<MemorySuggestionRecord> listByProject(String projectId) {
        return jdbcTemplate.query("""
                        SELECT id, scope, project_id, conversation_id, content, status, created_at, updated_at
                        FROM loom_memory_suggestions
                        WHERE project_id = ? OR project_id IS NULL
                        ORDER BY created_at DESC, id DESC
                        """,
                this::mapRow,
                projectId);
    }

    @Override
    public MemorySuggestionRecord updateStatus(String id, MemorySuggestionStatus status, String updatedAt) {
        MemorySuggestionRecord current = findById(id).orElseThrow(() -> new IllegalArgumentException("Suggestion not found: " + id));
        MemorySuggestionRecord updated = new MemorySuggestionRecord(
                current.id(),
                current.scope(),
                current.projectId(),
                current.conversationId(),
                current.content(),
                status.wireValue(),
                current.createdAt(),
                updatedAt
        );
        jdbcTemplate.update("""
                UPDATE loom_memory_suggestions
                SET status = ?, updated_at = ?
                WHERE id = ?
                """, updated.status(), updated.updatedAt(), id);
        return updated;
    }

    private MemorySuggestionRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new MemorySuggestionRecord(
                rs.getString("id"),
                rs.getString("scope"),
                rs.getString("project_id"),
                rs.getString("conversation_id"),
                rs.getString("content"),
                rs.getString("status"),
                rs.getString("created_at"),
                rs.getString("updated_at")
        );
    }
}

