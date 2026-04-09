package com.loom.server.memory;

import java.util.List;
import java.util.Optional;

public interface MemoryItemRepository {
    MemoryItemRecord save(MemoryItemRecord record);

    Optional<MemoryItemRecord> findById(String id);

    List<MemoryItemRecord> listByProject(String projectId);

    void deleteById(String id);
}
