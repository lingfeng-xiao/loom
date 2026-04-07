package com.template.node.dto;

import com.template.node.model.ProbeStatus;

public record NodeHeartbeatProbeRequest(
        String name,
        String kind,
        String target,
        ProbeStatus status,
        String detail
) {
}
