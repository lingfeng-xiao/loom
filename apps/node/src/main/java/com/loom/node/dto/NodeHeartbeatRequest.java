package com.loom.node.dto;

import com.loom.node.model.ProbeStatus;

import java.util.List;

public record NodeHeartbeatRequest(ProbeStatus status, List<NodeHeartbeatProbeRequest> probes) {
}
