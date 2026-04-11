package com.loom.node.service;

import com.loom.node.config.NodeProperties;
import com.loom.node.model.NodeProbeResult;
import com.loom.node.model.ProbeStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class NodeProbeService {

    private final NodeProperties nodeProperties;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public NodeProbeService(NodeProperties nodeProperties) {
        this.nodeProperties = nodeProperties;
    }

    public List<NodeProbeResult> runProbes() {
        List<NodeProbeResult> results = new ArrayList<>();
        for (NodeProperties.Probe probe : nodeProperties.getServiceProbes()) {
            results.add(runProbe(probe));
        }
        return results;
    }

    private NodeProbeResult runProbe(NodeProperties.Probe probe) {
        Instant now = Instant.now();
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(probe.getTarget()))
                    .GET()
                    .timeout(Duration.ofMillis(probe.getTimeoutMs()))
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            ProbeStatus status = response.statusCode() == probe.getExpectedStatus() ? ProbeStatus.up : ProbeStatus.degraded;
            return new NodeProbeResult(
                    probe.getName(),
                    probe.getKind(),
                    probe.getTarget(),
                    status,
                    "HTTP " + response.statusCode(),
                    now
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new NodeProbeResult(
                    probe.getName(),
                    probe.getKind(),
                    probe.getTarget(),
                    ProbeStatus.down,
                    exception.getMessage(),
                    now
            );
        } catch (IOException exception) {
            return new NodeProbeResult(
                    probe.getName(),
                    probe.getKind(),
                    probe.getTarget(),
                    ProbeStatus.down,
                    exception.getMessage(),
                    now
            );
        }
    }
}
