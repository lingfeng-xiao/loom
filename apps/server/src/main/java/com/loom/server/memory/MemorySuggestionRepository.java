package com.loom.server.memory;

import java.util.List;
import java.util.Optional;

public interface MemorySuggestionRepository {
    MemorySuggestionRecord save(MemorySuggestionRecord record);

    Optional<MemorySuggestionRecord> findById(String id);

    List<MemorySuggestionRecord> listByProject(String projectId);

    MemorySuggestionRecord updateStatus(String id, MemorySuggestionStatus status, String updatedAt);
}
