package com.loom.node.model;

import java.time.Instant;
import java.util.List;

public record NodeSnapshot(String hostname, ProbeStatus status, List<NodeProbeResult> probes, Instant recordedAt) {
}
