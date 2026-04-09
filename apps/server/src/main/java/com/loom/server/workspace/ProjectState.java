package com.loom.server.workspace;

import com.loom.server.workspace.WorkspaceDtos.CapabilityBindingSummary;

import java.util.ArrayList;
import java.util.List;

final class ProjectState {
    final String id;
    String name;
    String description;
    String status;
    String instructions;
    final List<String> pinnedConversationIds;
    final CapabilityBindingSummary bindings;
    String lastMessageAt;
    String updatedAt;

    ProjectState(
            String id,
            String name,
            String description,
            String status,
            String instructions,
            List<String> pinnedConversationIds,
            CapabilityBindingSummary bindings,
            String lastMessageAt,
            String updatedAt
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.status = status;
        this.instructions = instructions;
        this.pinnedConversationIds = new ArrayList<>(pinnedConversationIds);
        this.bindings = bindings;
        this.lastMessageAt = lastMessageAt;
        this.updatedAt = updatedAt;
    }
}
