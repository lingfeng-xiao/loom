package com.loom.server.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.loom.server.model.Asset;
import com.loom.server.model.AssetType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class AssetRepository extends JdbcRepositorySupport {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    public AssetRepository(JdbcClient jdbcClient, JdbcJsonSupport jsonSupport) {
        super(jdbcClient, jsonSupport);
    }

    public List<Asset> findAll() {
        return jdbcClient.sql("select * from assets order by created_at desc")
                .query(this::mapRow)
                .list();
    }

    public List<Asset> findByProject(String projectId) {
        return jdbcClient.sql("select * from assets where project_id = :projectId order by created_at desc")
                .param("projectId", projectId)
                .query(this::mapRow)
                .list();
    }

    public Optional<Asset> findById(String id) {
        return jdbcClient.sql("select * from assets where id = :id")
                .param("id", id)
                .query(this::mapRow)
                .optional();
    }

    public void save(Asset asset) {
        jdbcClient.sql("""
                insert into assets (
                    id, project_id, type, title, content_ref, storage_path, source_conversation_id, source_plan_id, source_node_id, tags, created_at
                ) values (
                    :id, :projectId, :type, :title, :contentRef, :storagePath, :sourceConversationId, :sourcePlanId, :sourceNodeId, :tags, :createdAt
                )
                on duplicate key update
                    type = values(type),
                    title = values(title),
                    content_ref = values(content_ref),
                    storage_path = values(storage_path),
                    source_conversation_id = values(source_conversation_id),
                    source_plan_id = values(source_plan_id),
                    source_node_id = values(source_node_id),
                    tags = values(tags)
                """)
                .param("id", asset.id())
                .param("projectId", asset.projectId())
                .param("type", asset.type().value())
                .param("title", asset.title())
                .param("contentRef", asset.contentRef())
                .param("storagePath", asset.storagePath())
                .param("sourceConversationId", asset.sourceConversationId())
                .param("sourcePlanId", asset.sourcePlanId())
                .param("sourceNodeId", asset.sourceNodeId())
                .param("tags", jsonSupport.write(asset.tags()))
                .param("createdAt", asset.createdAt())
                .update();
    }

    private Asset mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        return new Asset(
                resultSet.getString("id"),
                resultSet.getString("project_id"),
                AssetType.fromValue(resultSet.getString("type")),
                resultSet.getString("title"),
                resultSet.getString("content_ref"),
                resultSet.getString("storage_path"),
                resultSet.getString("source_conversation_id"),
                resultSet.getString("source_plan_id"),
                resultSet.getString("source_node_id"),
                jsonSupport.read(resultSet.getString("tags"), STRING_LIST, List::of),
                instant(resultSet, "created_at")
        );
    }
}
