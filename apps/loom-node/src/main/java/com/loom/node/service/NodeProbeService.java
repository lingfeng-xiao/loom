package com.loom.node.service;

import com.loom.node.config.NodeProperties;
import com.loom.node.dto.NodeServiceStatus;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.Socket;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NodeProbeService {

    private final NodeProperties properties;
    private final HttpClient httpClient;

    public NodeProbeService(NodeProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public List<NodeServiceStatus> collectServiceStatuses() {
        List<NodeServiceStatus> services = new ArrayList<>();
        List<NodeProperties.ServiceProbe> probes = properties.getServiceProbes();
        if (probes != null && !probes.isEmpty()) {
            for (NodeProperties.ServiceProbe probe : probes) {
                services.add(probeService(probe));
            }
            return services;
        }

        for (String serviceName : properties.configuredServiceNames()) {
            services.add(new NodeServiceStatus(
                    serviceName,
                    "legacy",
                    serviceName,
                    "unknown",
                    "no structured probe configured",
                    Instant.now()
            ));
        }
        return services;
    }

    private NodeServiceStatus probeService(NodeProperties.ServiceProbe probe) {
        String kind = normalize(probe.getKind());
        return switch (kind) {
            case "tcp" -> probeTcp(probe);
            case "http" -> probeHttp(probe);
            default -> new NodeServiceStatus(
                    probe.getName(),
                    kind,
                    probe.getTarget(),
                    "unknown",
                    "unsupported probe kind",
                    Instant.now()
            );
        };
    }

    private NodeServiceStatus probeHttp(NodeProperties.ServiceProbe probe) {
        Instant recordedAt = Instant.now();
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(probe.getTarget()))
                    .timeout(Duration.ofMillis(probe.getTimeoutMs()))
                    .header("User-Agent", "loom-node")
                    .GET()
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            int expected = probe.getExpectedStatus() == null ? 200 : probe.getExpectedStatus();
            int actual = response.statusCode();
            boolean matched = actual == expected;
            return new NodeServiceStatus(
                    probe.getName(),
                    "http",
                    probe.getTarget(),
                    matched ? "online" : "degraded",
                    matched ? "http " + actual : "expected " + expected + " but got " + actual,
                    recordedAt
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new NodeServiceStatus(probe.getName(), "http", probe.getTarget(), "offline", "interrupted", recordedAt);
        } catch (Exception ex) {
            return new NodeServiceStatus(probe.getName(), "http", probe.getTarget(), "offline", ex.getMessage(), recordedAt);
        }
    }

    private NodeServiceStatus probeTcp(NodeProperties.ServiceProbe probe) {
        Instant recordedAt = Instant.now();
        try {
            TcpTarget target = parseTcpTarget(probe.getTarget());
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(target.host(), target.port()), (int) probe.getTimeoutMs());
            }
            return new NodeServiceStatus(
                    probe.getName(),
                    "tcp",
                    probe.getTarget(),
                    "online",
                    "tcp connection established",
                    recordedAt
            );
        } catch (Exception ex) {
            return new NodeServiceStatus(probe.getName(), "tcp", probe.getTarget(), "offline", ex.getMessage(), recordedAt);
        }
    }

    private TcpTarget parseTcpTarget(String target) {
        if (target == null || target.isBlank()) {
            throw new IllegalArgumentException("TCP target is blank");
        }

        String value = target.trim();
        if (value.startsWith("tcp://")) {
            URI uri = URI.create(value);
            if (uri.getHost() == null || uri.getPort() < 0) {
                throw new IllegalArgumentException("TCP target must include host and port");
            }
            return new TcpTarget(uri.getHost(), uri.getPort());
        }

        int separator = value.lastIndexOf(':');
        if (separator <= 0 || separator == value.length() - 1) {
            throw new IllegalArgumentException("TCP target must use host:port");
        }
        String host = value.substring(0, separator);
        int port = Integer.parseInt(value.substring(separator + 1));
        return new TcpTarget(host, port);
    }

    private String normalize(String kind) {
        return kind == null ? "unknown" : kind.trim().toLowerCase();
    }

    private record TcpTarget(String host, int port) {
    }
}
