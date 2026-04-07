package com.template.server.dto;

import com.template.server.model.ProbeStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record NodeHeartbeatRequest(
        @NotNull ProbeStatus status,
        @Valid List<NodeHeartbeatProbeRequest> probes
) {
}
