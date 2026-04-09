package com.loom.server.workspace;

import com.loom.server.workspace.WorkspaceDtos.ActionView;

import java.util.List;

final class ActionState {
    final String id;
    String projectId;
    final String conversationId;
    final String runId;
    final String title;
    String status;
    String summary;
    final String startedAt;
    String completedAt;
    final List<String> stepIds;

    ActionState(
            String id,
            String projectId,
            String conversationId,
            String runId,
            String title,
            String status,
            String summary,
            String startedAt,
            String completedAt,
            List<String> stepIds
    ) {
        this.id = id;
        this.projectId = projectId;
        this.conversationId = conversationId;
        this.runId = runId;
        this.title = title;
        this.status = status;
        this.summary = summary;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.stepIds = List.copyOf(stepIds);
    }

    ActionView toView() {
        return new ActionView(id, projectId, conversationId, runId, title, status, summary, startedAt, completedAt, stepIds);
    }
}
