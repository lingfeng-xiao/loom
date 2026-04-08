package com.loom.server.dto;

import com.loom.server.model.ProbeStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record NodeHeartbeatRequest(
        @NotNull ProbeStatus status,
        @Valid List<NodeHeartbeatProbeRequest> probes
) {
}
