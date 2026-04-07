package com.template.server.dto;

import com.template.server.model.ProbeStatus;
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
