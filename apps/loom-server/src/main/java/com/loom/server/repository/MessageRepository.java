package com.loom.server.repository;

import com.loom.server.model.Message;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class MessageRepository extends JdbcRepositorySupport {

    public MessageRepository(JdbcClient jdbcClient, JdbcJsonSupport jsonSupport) {
        super(jdbcClient, jsonSupport);
    }

    public List<Message> findByConversation(String conversationId) {
        return jdbcClient.sql("select * from messages where conversation_id = :conversationId order by created_at")
                .param("conversationId", conversationId)
                .query(this::mapRow)
                .list();
    }

    public void save(Message message) {
        jdbcClient.sql("""
                insert into messages (
                    id, conversation_id, project_id, role, content, created_at
                ) values (
                    :id, :conversationId, :projectId, :role, :content, :createdAt
                )
                on duplicate key update
                    role = values(role),
                    content = values(content)
                """)
                .param("id", message.id())
                .param("conversationId", message.conversationId())
                .param("projectId", message.projectId())
                .param("role", message.role())
                .param("content", message.content())
                .param("createdAt", message.createdAt())
                .update();
    }

    private Message mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        return new Message(
                resultSet.getString("id"),
                resultSet.getString("conversation_id"),
                resultSet.getString("project_id"),
                resultSet.getString("role"),
                resultSet.getString("content"),
                instant(resultSet, "created_at")
        );
    }
}
