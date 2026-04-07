package com.loom.server.model;

public record PlanStep(
        String id,
        String title,
        String description,
        PlanStepStatus status,
        String result,
        int sortOrder
) {
}
