package com.loom.node.dto;

import com.loom.node.model.ProbeStatus;

public record NodeHeartbeatProbeRequest(
        String name,
        String kind,
        String target,
        ProbeStatus status,
        String detail
) {
}
