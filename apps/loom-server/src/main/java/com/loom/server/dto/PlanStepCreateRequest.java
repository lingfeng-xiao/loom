package com.loom.server.dto;

public record PlanStepCreateRequest(
        String title,
        String description,
        Integer sortOrder
) {
}
