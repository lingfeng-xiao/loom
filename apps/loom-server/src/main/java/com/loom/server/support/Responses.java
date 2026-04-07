package com.loom.server.support;

import com.loom.server.model.Asset;
import com.loom.server.model.AuditLogEntry;
import com.loom.server.model.CommandId;
import com.loom.server.model.Conversation;
import com.loom.server.model.Memory;
import com.loom.server.model.Message;
import com.loom.server.model.Node;
import com.loom.server.model.Plan;
import com.loom.server.model.Project;
import com.loom.server.model.Skill;
import com.loom.server.model.WorkspaceSettings;
import java.util.List;

public final class Responses {
    private Responses() {
    }

    public record DashboardResponse(
            Project project,
            List<Conversation> conversations,
            List<Memory> memories,
            List<Asset> recentAssets,
            List<Node> nodes,
            Plan activePlan
    ) {
    }

    public record BootstrapResponse(
            List<Project> projects,
            List<Skill> skills,
            List<Node> nodes,
            List<CommandId> commands,
            String defaultProjectId,
            WorkspaceSettings workspaceSettings
    ) {
    }

    public record CommandExecutionResult(
            String commandId,
            String message,
            String projectId,
            String conversationId,
            String planId,
            String assetId,
            List<String> memoryIds,
            List<Project> projects,
            List<Conversation> conversations,
            List<Memory> memories,
            List<Asset> assets,
            List<Node> nodes,
            List<Skill> skills,
            List<AuditLogEntry> logs
    ) {
    }

    public record SkillInvocationResult(
            String skillId,
            String message,
            String assetId,
            String output
    ) {
    }
}
