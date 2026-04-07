package com.loom.server.model;

import java.time.Instant;
import java.util.List;

public record Plan(
        String id,
        String projectId,
        String conversationId,
        String goal,
        List<String> constraints,
        PlanStatus status,
        boolean approvalRequired,
        List<PlanStep> steps,
        PlanExecutionResult executionResult,
        Instant createdAt,
        Instant updatedAt
) {
}
