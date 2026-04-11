package com.loom.server.dto;

import com.loom.server.model.ProbeStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NodeHeartbeatProbeRequest(
        @NotBlank String name,
        @NotBlank String kind,
        @NotBlank String target,
        @NotNull ProbeStatus status,
        String detail
) {
}
