package com.loom.server.workspace;

import com.loom.server.workspace.WorkspaceDtos.MessageView;
import com.loom.server.workspace.WorkspaceDtos.RunStepView;

import java.util.ArrayList;
import java.util.List;

final class ConversationState {
    final String id;
    String projectId;
    String title;
    String summary;
    String mode;
    String status;
    boolean pinned;
    String updatedAt;
    String lastMessageAt;
    String activeActionId;
    String activeRunId;
    String traceSummary;
    String traceUpdatedAt;
    final ContextState context;
    final List<MessageView> messages;
    final List<RunStepView> traceSteps;
    PendingAssistantTurn pendingTurn;

    ConversationState(String id, String projectId, String title, String mode, boolean pinned, String createdAt) {
        this.id = id;
        this.projectId = projectId;
        this.title = title;
        this.summary = "Waiting for the first message.";
        this.mode = mode;
        this.status = "idle";
        this.pinned = pinned;
        this.updatedAt = createdAt;
        this.lastMessageAt = createdAt;
        this.traceSummary = "Trace is ready to show the next execution path.";
        this.traceUpdatedAt = createdAt;
        this.context = new ContextState();
        this.messages = new ArrayList<>();
        this.traceSteps = new ArrayList<>();
    }
}
