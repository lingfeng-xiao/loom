package com.loom.server.repository;

import com.loom.server.model.Conversation;
import com.loom.server.model.ConversationMode;
import com.loom.server.model.ConversationStatus;
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
public class ConversationRepository extends JdbcRepositorySupport {

    public ConversationRepository(JdbcClient jdbcClient, JdbcJsonSupport jsonSupport) {
        super(jdbcClient, jsonSupport);
    }

    public List<Conversation> findByProject(String projectId, String modeValue, String statusValue, String query) {
        StringBuilder sql = new StringBuilder("select * from conversations where project_id = :projectId");
        Map<String, Object> params = new HashMap<>();
        params.put("projectId", projectId);
        if (modeValue != null && !modeValue.isBlank()) {
            sql.append(" and mode = :mode");
            params.put("mode", modeValue.toLowerCase(Locale.ROOT));
        }
        if (statusValue != null && !statusValue.isBlank()) {
            sql.append(" and status = :status");
            params.put("status", statusValue.toLowerCase(Locale.ROOT));
        }
        if (query != null && !query.isBlank()) {
            sql.append(" and (lower(title) like :query or lower(summary) like :query)");
            params.put("query", "%" + query.toLowerCase(Locale.ROOT) + "%");
        }
        sql.append(" order by updated_at desc");
        return jdbcClient.sql(sql.toString()).params(params).query(this::mapRow).list();
    }

    public Optional<Conversation> findById(String id) {
        return jdbcClient.sql("select * from conversations where id = :id")
                .param("id", id)
                .query(this::mapRow)
                .optional();
    }

    public void save(Conversation conversation) {
        jdbcClient.sql("""
                insert into conversations (
                    id, project_id, title, mode, status, summary, created_at, updated_at
                ) values (
                    :id, :projectId, :title, :mode, :status, :summary, :createdAt, :updatedAt
                )
                on duplicate key update
                    title = values(title),
                    mode = values(mode),
                    status = values(status),
                    summary = values(summary),
                    updated_at = values(updated_at)
                """)
                .param("id", conversation.id())
                .param("projectId", conversation.projectId())
                .param("title", conversation.title())
                .param("mode", conversation.mode().value())
                .param("status", conversation.status().value())
                .param("summary", conversation.summary())
                .param("createdAt", conversation.createdAt())
                .param("updatedAt", conversation.updatedAt())
                .update();
    }

    private Conversation mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        return new Conversation(
                resultSet.getString("id"),
                resultSet.getString("project_id"),
                resultSet.getString("title"),
                ConversationMode.fromValue(resultSet.getString("mode")),
                ConversationStatus.fromValue(resultSet.getString("status")),
                resultSet.getString("summary"),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at")
        );
    }
}
