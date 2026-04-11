package com.loom.node.service;

import com.loom.node.config.NodeProperties;
import com.loom.node.model.NodeProbeResult;
import com.loom.node.model.NodeSnapshot;
import com.loom.node.model.ProbeStatus;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.List;

@Service
public class NodeSnapshotService implements NodeSnapshotProvider {

    private final NodeProbeService nodeProbeService;
    private final NodeProperties nodeProperties;

    public NodeSnapshotService(NodeProbeService nodeProbeService, NodeProperties nodeProperties) {
        this.nodeProbeService = nodeProbeService;
        this.nodeProperties = nodeProperties;
    }

    @Override
    public NodeSnapshot captureSnapshot() {
        List<NodeProbeResult> probes = nodeProbeService.runProbes();
        ProbeStatus overallStatus = probes.stream().allMatch(probe -> probe.status() == ProbeStatus.up)
                ? ProbeStatus.up
                : probes.stream().anyMatch(probe -> probe.status() == ProbeStatus.down) ? ProbeStatus.down : ProbeStatus.degraded;
        return new NodeSnapshot(resolveHostname(), overallStatus, probes, Instant.now());
    }

    private String resolveHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException exception) {
            return nodeProperties.getHost();
        }
    }
}
