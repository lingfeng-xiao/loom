package com.loom.server.dto;

import com.loom.server.model.NodeServiceStatus;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record NodeSnapshotRequest(
        @NotBlank String hostname,
        String osName,
        double cpuUsage,
        double processCpuUsage,
        double memoryUsage,
        long totalMemoryBytes,
        long usedMemoryBytes,
        long freeMemoryBytes,
        double diskUsage,
        long totalDiskBytes,
        long usedDiskBytes,
        long freeDiskBytes,
        List<NodeServiceStatus> services
) {
}
