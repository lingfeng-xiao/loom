package com.loom.node.dto;

import java.time.Instant;
import java.util.List;

public record NodeSnapshot(
        String hostname,
        String osName,
        double cpuUsagePercent,
        double processCpuUsagePercent,
        long totalMemoryBytes,
        long usedMemoryBytes,
        long freeMemoryBytes,
        double diskUsagePercent,
        List<NodeServiceStatus> services,
        Instant recordedAt
) {
}
