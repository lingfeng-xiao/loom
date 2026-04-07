package com.loom.node.service;

import com.loom.node.config.NodeProperties;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import com.sun.net.httpserver.HttpServer;

import static org.assertj.core.api.Assertions.assertThat;

class NodeSnapshotServiceTest {

    @Test
    void collectsSnapshotWithExpectedShape() throws Exception {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        httpServer.createContext("/health", exchange -> {
            byte[] body = "{\"status\":\"ok\"}".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        httpServer.setExecutor(Executors.newSingleThreadExecutor());
        httpServer.start();

        ServerSocket tcpServer = new ServerSocket(0);
        var tcpExecutor = Executors.newSingleThreadExecutor();
        tcpExecutor.submit(() -> {
            try (var socket = tcpServer.accept()) {
                // accept one probe connection
            } catch (Exception ignored) {
            }
        });

        NodeProperties properties = new NodeProperties();
        properties.setName("loom-node");
        properties.setType("server");
        properties.setHost("localhost");
        properties.setServerBaseUrl("http://localhost:8080");
        properties.setServerToken("token");
        List<NodeProperties.ServiceProbe> probes = new ArrayList<>();
        probes.add(probe("api", "http", "http://127.0.0.1:" + httpServer.getAddress().getPort() + "/health", 200));
        probes.add(probe("cache", "tcp", "127.0.0.1:" + tcpServer.getLocalPort(), 200));
        probes.add(probe("broken", "http", "http://127.0.0.1:" + httpServer.getAddress().getPort() + "/health", 201));
        properties.setServiceProbes(probes);

        NodeProbeService probeService = new NodeProbeService(properties);
        NodeSnapshotService service = new NodeSnapshotService(properties, probeService);

        try {
            var snapshot = service.collect();

            assertThat(snapshot.hostname()).isNotBlank();
            assertThat(snapshot.osName()).isNotBlank();
            assertThat(snapshot.totalMemoryBytes()).isPositive();
            assertThat(snapshot.usedMemoryBytes()).isGreaterThanOrEqualTo(0);
            assertThat(snapshot.freeMemoryBytes()).isGreaterThanOrEqualTo(0);
            assertThat(snapshot.diskUsagePercent()).isGreaterThanOrEqualTo(-1.0);
            assertThat(snapshot.services()).hasSize(3);
            assertThat(snapshot.services()).anySatisfy(status -> {
                assertThat(status.name()).isEqualTo("api");
                assertThat(status.kind()).isEqualTo("http");
                assertThat(status.status()).isEqualTo("online");
            });
            assertThat(snapshot.services()).anySatisfy(status -> {
                assertThat(status.name()).isEqualTo("cache");
                assertThat(status.status()).isEqualTo("online");
            });
            assertThat(snapshot.services()).anySatisfy(status -> {
                assertThat(status.name()).isEqualTo("broken");
                assertThat(status.status()).isEqualTo("degraded");
            });
            assertThat(snapshot.recordedAt()).isNotNull();
        } finally {
            httpServer.stop(0);
            tcpServer.close();
            tcpExecutor.shutdownNow();
        }
    }

    private NodeProperties.ServiceProbe probe(String name, String kind, String target, int expectedStatus) {
        NodeProperties.ServiceProbe probe = new NodeProperties.ServiceProbe();
        probe.setName(name);
        probe.setKind(kind);
        probe.setTarget(target);
        probe.setTimeoutMs(3000);
        probe.setExpectedStatus(expectedStatus);
        return probe;
    }
}
