package com.loom.server.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.time.Instant;
import java.util.List;

public record NodeSnapshot(
        String hostname,
        String osName,
        @JsonAlias("cpuUsagePercent") double cpuUsage,
        @JsonAlias("processCpuUsagePercent") double processCpuUsage,
        double memoryUsage,
        long totalMemoryBytes,
        long usedMemoryBytes,
        long freeMemoryBytes,
        @JsonAlias("diskUsagePercent") double diskUsage,
        long totalDiskBytes,
        long usedDiskBytes,
        long freeDiskBytes,
        List<NodeServiceStatus> services,
        Instant recordedAt
) {
}
