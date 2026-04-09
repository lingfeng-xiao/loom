package com.loom.server.workspace;

final class PendingAssistantTurn {
    final String projectId;
    final String conversationId;
    final String actionId;
    final String runId;
    final String userInput;
    final boolean allowMemory;
    final String startedAt;
    final String thinkingMessageId;
    final String assistantMessageId;
    boolean streamingStarted;

    PendingAssistantTurn(
            String projectId,
            String conversationId,
            String actionId,
            String runId,
            String userInput,
            boolean allowMemory,
            String startedAt,
            String thinkingMessageId,
            String assistantMessageId
    ) {
        this.projectId = projectId;
        this.conversationId = conversationId;
        this.actionId = actionId;
        this.runId = runId;
        this.userInput = userInput;
        this.allowMemory = allowMemory;
        this.startedAt = startedAt;
        this.thinkingMessageId = thinkingMessageId;
        this.assistantMessageId = assistantMessageId;
    }
}
