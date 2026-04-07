package com.loom.server.service;

import com.loom.server.config.LoomProperties;
import com.loom.server.dto.NodeHeartbeatRequest;
import com.loom.server.dto.NodeRegisterRequest;
import com.loom.server.dto.NodeSnapshotRequest;
import com.loom.server.error.NotFoundException;
import com.loom.server.model.Node;
import com.loom.server.model.NodeServiceStatus;
import com.loom.server.model.NodeSnapshot;
import com.loom.server.model.NodeStatus;
import com.loom.server.repository.NodeRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class NodeService {

    private final NodeRepository nodeRepository;
    private final LoomProperties properties;
    private final AuditService auditService;

    public NodeService(NodeRepository nodeRepository, LoomProperties properties, AuditService auditService) {
        this.nodeRepository = nodeRepository;
        this.properties = properties;
        this.auditService = auditService;
    }

    public List<Node> listNodes() {
        return nodeRepository.findAll().stream()
                .map(this::normalizeNode)
                .sorted(Comparator.comparing(Node::updatedAt).reversed())
                .toList();
    }

    public Node getNode(String id) {
        Node node = nodeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("未找到节点 " + id));
        return normalizeNode(node);
    }

    public Node registerNode(NodeRegisterRequest request) {
        Instant now = Instant.now();
        Node current = nodeRepository.findByNameAndHost(request.name(), request.host()).orElse(null);
        Node node = new Node(
                current == null ? UUID.randomUUID().toString() : current.id(),
                request.name(),
                request.type(),
                request.host(),
                request.tags() == null ? List.of() : List.copyOf(request.tags()),
                NodeStatus.ONLINE,
                current == null ? null : current.lastHeartbeat(),
                current == null ? null : current.snapshot(),
                request.capabilities() == null ? List.of() : List.copyOf(request.capabilities()),
                current == null ? null : current.lastError(),
                current == null ? now : current.createdAt(),
                now
        );
        nodeRepository.save(node);
        auditService.record("system", "node-service", "node", node.id(), "register", request.host(), "ok");
        return normalizeNode(node);
    }

    public Node heartbeat(String id, NodeHeartbeatRequest request) {
        Node current = getNode(id);
        NodeSnapshot snapshot = normalizeSnapshot(request == null ? null : request.snapshot());
        Instant heartbeatAt = request != null && request.sentAt() != null ? request.sentAt() : Instant.now();
        String lastError = request != null && request.lastError() != null && !request.lastError().isBlank()
                ? request.lastError()
                : deriveLastError(snapshot);
        Node updated = new Node(
                current.id(),
                current.name(),
                current.type(),
                current.host(),
                current.tags(),
                deriveStatus(snapshot, heartbeatAt),
                heartbeatAt,
                snapshot == null ? current.snapshot() : snapshot,
                current.capabilities(),
                lastError,
                current.createdAt(),
                Instant.now()
        );
        nodeRepository.save(updated);
        auditService.record("system", "node-service", "node", id, "heartbeat", current.host(), "ok");
        return normalizeNode(updated);
    }

    public Node snapshot(String id, NodeSnapshotRequest request) {
        Node current = getNode(id);
        NodeSnapshot snapshot = normalizeSnapshot(new NodeSnapshot(
                request.hostname(),
                request.osName(),
                request.cpuUsage(),
                request.processCpuUsage(),
                request.memoryUsage(),
                request.totalMemoryBytes(),
                request.usedMemoryBytes(),
                request.freeMemoryBytes(),
                request.diskUsage(),
                request.totalDiskBytes(),
                request.usedDiskBytes(),
                request.freeDiskBytes(),
                request.services() == null ? List.of() : List.copyOf(request.services()),
                Instant.now()
        ));
        Node updated = new Node(
                current.id(),
                current.name(),
                current.type(),
                current.host(),
                current.tags(),
                deriveStatus(snapshot, Instant.now()),
                Instant.now(),
                snapshot,
                current.capabilities(),
                deriveLastError(snapshot),
                current.createdAt(),
                Instant.now()
        );
        nodeRepository.save(updated);
        auditService.record("system", "node-service", "node", id, "snapshot", request.hostname(), "ok");
        return normalizeNode(updated);
    }

    private Node normalizeNode(Node node) {
        NodeSnapshot snapshot = normalizeSnapshot(node.snapshot());
        NodeStatus status = snapshot == null ? normalizeStatusByHeartbeat(node.status(), node.lastHeartbeat()) : deriveStatus(snapshot, node.lastHeartbeat());
        return new Node(
                node.id(),
                node.name(),
                node.type(),
                node.host(),
                node.tags(),
                status,
                node.lastHeartbeat(),
                snapshot,
                node.capabilities(),
                node.lastError() == null || node.lastError().isBlank() ? deriveLastError(snapshot) : node.lastError(),
                node.createdAt(),
                node.updatedAt()
        );
    }

    private NodeSnapshot normalizeSnapshot(NodeSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        long totalMemoryBytes = snapshot.totalMemoryBytes();
        long usedMemoryBytes = snapshot.usedMemoryBytes();
        long freeMemoryBytes = snapshot.freeMemoryBytes();
        double memoryUsage = snapshot.memoryUsage();
        if (memoryUsage <= 0 && totalMemoryBytes > 0) {
            memoryUsage = (double) usedMemoryBytes / totalMemoryBytes;
        } else if (memoryUsage > 1.0) {
            memoryUsage = memoryUsage / 100.0;
        }

        long totalDiskBytes = snapshot.totalDiskBytes();
        long usedDiskBytes = snapshot.usedDiskBytes();
        long freeDiskBytes = snapshot.freeDiskBytes();
        double diskUsage = snapshot.diskUsage();
        if (diskUsage > 1.0) {
            diskUsage = diskUsage / 100.0;
        }
        if (totalDiskBytes == 0 && usedDiskBytes == 0 && freeDiskBytes == 0 && diskUsage >= 0) {
            totalDiskBytes = -1;
            usedDiskBytes = -1;
            freeDiskBytes = -1;
        }

        double cpuUsage = snapshot.cpuUsage() > 1.0 ? snapshot.cpuUsage() / 100.0 : snapshot.cpuUsage();
        double processCpuUsage = snapshot.processCpuUsage() > 1.0 ? snapshot.processCpuUsage() / 100.0 : snapshot.processCpuUsage();

        return new NodeSnapshot(
                snapshot.hostname(),
                snapshot.osName(),
                cpuUsage,
                processCpuUsage,
                memoryUsage,
                totalMemoryBytes,
                usedMemoryBytes,
                freeMemoryBytes,
                diskUsage,
                totalDiskBytes,
                usedDiskBytes,
                freeDiskBytes,
                snapshot.services() == null ? List.of() : snapshot.services(),
                snapshot.recordedAt()
        );
    }

    private NodeStatus deriveStatus(NodeSnapshot snapshot, Instant lastHeartbeat) {
        NodeStatus byHeartbeat = normalizeStatusByHeartbeat(NodeStatus.ONLINE, lastHeartbeat);
        if (byHeartbeat == NodeStatus.OFFLINE || snapshot == null) {
            return byHeartbeat;
        }
        boolean hasOfflineService = snapshot.services().stream().anyMatch(service -> matches(service, "offline", "down"));
        if (hasOfflineService) {
            return NodeStatus.DEGRADED;
        }
        boolean hasDegradedService = snapshot.services().stream().anyMatch(service -> matches(service, "degraded"));
        if (hasDegradedService) {
            return NodeStatus.DEGRADED;
        }
        return NodeStatus.ONLINE;
    }

    private NodeStatus normalizeStatusByHeartbeat(NodeStatus fallback, Instant lastHeartbeat) {
        if (lastHeartbeat == null) {
            return NodeStatus.UNKNOWN;
        }
        Instant cutoff = Instant.now().minusSeconds(properties.getNodes().getHeartbeatTimeoutSeconds());
        if (lastHeartbeat.isBefore(cutoff)) {
            return NodeStatus.OFFLINE;
        }
        return fallback == null ? NodeStatus.UNKNOWN : fallback;
    }

    private String deriveLastError(NodeSnapshot snapshot) {
        if (snapshot == null || snapshot.services() == null) {
            return null;
        }
        return snapshot.services().stream()
                .filter(service -> matches(service, "offline", "down", "degraded"))
                .map(service -> {
                    String detail = service.detail() == null || service.detail().isBlank() ? service.status() : service.detail();
                    return service.name() + ": " + detail;
                })
                .findFirst()
                .orElse(null);
    }

    private boolean matches(NodeServiceStatus service, String... candidates) {
        if (service == null || service.status() == null) {
            return false;
        }
        String normalized = service.status().toLowerCase(Locale.ROOT);
        for (String candidate : candidates) {
            if (normalized.equals(candidate)) {
                return true;
            }
        }
        return false;
    }
}
