package com.loom.node.service;

import com.loom.node.config.NodeProperties;
import com.loom.node.dto.NodeServiceStatus;
import com.loom.node.dto.NodeSnapshot;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NodeSnapshotService {

    private final NodeProperties properties;
    private final NodeProbeService probeService;

    public NodeSnapshotService(NodeProperties properties, NodeProbeService probeService) {
        this.properties = properties;
        this.probeService = probeService;
    }

    public NodeSnapshot collect() {
        double cpuUsage = currentSystemCpuUsagePercent();
        double processCpuUsage = currentProcessCpuUsagePercent();
        long totalMemory = totalPhysicalMemoryBytes();
        long freeMemory = freePhysicalMemoryBytes(totalMemory);
        long usedMemory = Math.max(0, totalMemory - freeMemory);
        double diskUsage = currentDiskUsagePercent();
        List<NodeServiceStatus> services = probeService.collectServiceStatuses();

        return new NodeSnapshot(
                resolveHostname(),
                System.getProperty("os.name", "unknown"),
                cpuUsage,
                processCpuUsage,
                totalMemory,
                usedMemory,
                freeMemory,
                diskUsage,
                services,
                Instant.now()
        );
    }

    private double currentSystemCpuUsagePercent() {
        var mxBean = operatingSystemBean();
        if (mxBean == null) {
            return -1.0;
        }
        double load = mxBean.getSystemCpuLoad();
        return load < 0 ? -1.0 : roundToPercent(load * 100.0);
    }

    private double currentProcessCpuUsagePercent() {
        var mxBean = operatingSystemBean();
        if (mxBean == null) {
            return -1.0;
        }
        double load = mxBean.getProcessCpuLoad();
        return load < 0 ? -1.0 : roundToPercent(load * 100.0);
    }

    private long totalPhysicalMemoryBytes() {
        var mxBean = operatingSystemBean();
        if (mxBean == null) {
            return Runtime.getRuntime().maxMemory();
        }
        long total = mxBean.getTotalMemorySize();
        return total > 0 ? total : Runtime.getRuntime().maxMemory();
    }

    private long freePhysicalMemoryBytes(long fallbackTotal) {
        var mxBean = operatingSystemBean();
        if (mxBean == null) {
            return Math.max(0, Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory());
        }
        long free = mxBean.getFreeMemorySize();
        return free >= 0 ? free : Math.max(0, fallbackTotal - Runtime.getRuntime().totalMemory());
    }

    private double currentDiskUsagePercent() {
        try {
            Path workingDir = Path.of(System.getProperty("user.dir", "."));
            FileStore store = Files.getFileStore(workingDir);
            long total = store.getTotalSpace();
            long usable = store.getUsableSpace();
            if (total <= 0) {
                return -1.0;
            }
            double usedPercent = (double) (total - usable) * 100.0 / total;
            return roundToPercent(usedPercent);
        } catch (Exception ex) {
            return -1.0;
        }
    }

    private com.sun.management.OperatingSystemMXBean operatingSystemBean() {
        return ManagementFactory.getPlatformMXBean(com.sun.management.OperatingSystemMXBean.class);
    }

    private String resolveHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            return properties.getHost() != null ? properties.getHost() : new File(".").getAbsolutePath();
        }
    }

    private double roundToPercent(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
