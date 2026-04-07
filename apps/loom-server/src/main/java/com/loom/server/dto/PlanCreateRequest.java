package com.loom.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record PlanCreateRequest(
        @NotBlank String projectId,
        @NotBlank String conversationId,
        @NotBlank String goal,
        List<String> constraints,
        @NotNull Boolean approvalRequired,
        List<PlanStepCreateRequest> steps
) {
}
