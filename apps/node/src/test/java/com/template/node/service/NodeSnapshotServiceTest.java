package com.template.node.service;

import com.sun.net.httpserver.HttpServer;
import com.template.node.config.NodeProperties;
import com.template.node.model.NodeSnapshot;
import com.template.node.model.ProbeStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;

class NodeSnapshotServiceTest {

    @Test
    void capturesProbeResultsAndOverallStatus() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/health", exchange -> {
            exchange.sendResponseHeaders(200, 2);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write("OK".getBytes());
            }
        });
        server.start();

        try {
            NodeProperties properties = new NodeProperties();
            NodeProperties.Probe successProbe = new NodeProperties.Probe();
            successProbe.setName("server");
            successProbe.setTarget("http://127.0.0.1:" + server.getAddress().getPort() + "/health");
            NodeProperties.Probe failingProbe = new NodeProperties.Probe();
            failingProbe.setName("missing");
            failingProbe.setTarget("http://127.0.0.1:1/health");
            properties.setServiceProbes(java.util.List.of(successProbe, failingProbe));

            NodeSnapshotService snapshotService = new NodeSnapshotService(new NodeProbeService(properties), properties);
            NodeSnapshot snapshot = snapshotService.captureSnapshot();

            assertThat(snapshot.probes()).hasSize(2);
            assertThat(snapshot.probes().get(0).status()).isEqualTo(ProbeStatus.up);
            assertThat(snapshot.status()).isEqualTo(ProbeStatus.down);
        } finally {
            server.stop(0);
        }
    }
}
