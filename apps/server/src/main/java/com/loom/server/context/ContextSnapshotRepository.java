package com.loom.server.context;

import java.util.List;
import java.util.Optional;

public interface ContextSnapshotRepository {
    ContextSnapshotRecord save(ContextSnapshotRecord record);

    List<ContextSnapshotRecord> listByConversation(String projectId, String conversationId);

    List<ContextSnapshotRecord> listByProject(String projectId);

    Optional<ContextSnapshotRecord> findLatestByConversation(String projectId, String conversationId);
}
