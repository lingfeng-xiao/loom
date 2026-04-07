package com.template.node.dto;

import com.template.node.model.ProbeStatus;

import java.util.List;

public record NodeHeartbeatRequest(ProbeStatus status, List<NodeHeartbeatProbeRequest> probes) {
}
