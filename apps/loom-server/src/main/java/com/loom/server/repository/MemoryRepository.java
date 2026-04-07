package com.loom.server.repository;

import com.loom.server.model.Memory;
import com.loom.server.model.MemoryScope;
import com.loom.server.model.MemoryStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class MemoryRepository extends JdbcRepositorySupport {

    public MemoryRepository(JdbcClient jdbcClient, JdbcJsonSupport jsonSupport) {
        super(jdbcClient, jsonSupport);
    }

    public List<Memory> findVisible(String projectId, String scopeValue) {
        StringBuilder sql = new StringBuilder("""
                select * from memories
                where (:projectId is null or project_id is null or project_id = :projectId)
                """);
        Map<String, Object> params = new HashMap<>();
        params.put("projectId", projectId);
        if (scopeValue != null && !scopeValue.isBlank()) {
            sql.append(" and scope = :scope");
            params.put("scope", scopeValue.toLowerCase(Locale.ROOT));
        }
        sql.append(" order by updated_at desc");
        return jdbcClient.sql(sql.toString()).params(params).query(this::mapRow).list();
    }

    public Optional<Memory> findById(String id) {
        return jdbcClient.sql("select * from memories where id = :id")
                .param("id", id)
                .query(this::mapRow)
                .optional();
    }

    public void save(Memory memory) {
        jdbcClient.sql("""
                insert into memories (
                    id, scope, project_id, title, content, priority, status, source_type, source_ref, created_at, updated_at
                ) values (
                    :id, :scope, :projectId, :title, :content, :priority, :status, :sourceType, :sourceRef, :createdAt, :updatedAt
                )
                on duplicate key update
                    scope = values(scope),
                    project_id = values(project_id),
                    title = values(title),
                    content = values(content),
                    priority = values(priority),
                    status = values(status),
                    source_type = values(source_type),
                    source_ref = values(source_ref),
                    updated_at = values(updated_at)
                """)
                .param("id", memory.id())
                .param("scope", memory.scope().value())
                .param("projectId", memory.projectId())
                .param("title", memory.title())
                .param("content", memory.content())
                .param("priority", memory.priority())
                .param("status", memory.status().value())
                .param("sourceType", memory.sourceType())
                .param("sourceRef", memory.sourceRef())
                .param("createdAt", memory.createdAt())
                .param("updatedAt", memory.updatedAt())
                .update();
    }

    private Memory mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        return new Memory(
                resultSet.getString("id"),
                MemoryScope.fromValue(resultSet.getString("scope")),
                resultSet.getString("project_id"),
                resultSet.getString("title"),
                resultSet.getString("content"),
                resultSet.getInt("priority"),
                MemoryStatus.fromValue(resultSet.getString("status")),
                resultSet.getString("source_type"),
                resultSet.getString("source_ref"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at")
        );
    }
}
