package com.loom.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.loom.server.dto.NodeHeartbeatRequest;
import com.loom.server.dto.NodeRegisterRequest;
import com.loom.server.model.NodeServiceStatus;
import com.loom.server.model.NodeSnapshot;
import com.loom.server.model.NodeStatus;
import com.loom.server.model.NodeType;
import com.loom.server.service.NodeService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class NodeStateIntegrationTest extends AbstractMySqlIntegrationTest {

    @Autowired
    private NodeService nodeService;

    @Test
    void marksNodeDegradedWhenAnyProbeIsDegraded() {
        var node = nodeService.registerNode(new NodeRegisterRequest(
                "probe-node",
                NodeType.SERVER,
                "127.0.0.1",
                List.of("integration"),
                List.of("probe:http")
        ));

        var updated = nodeService.heartbeat(node.id(), new NodeHeartbeatRequest(
                node.id(),
                node.name(),
                new NodeSnapshot(
                        "probe-node",
                        "Linux",
                        0.20,
                        0.10,
                        0.35,
                        2048,
                        717,
                        1331,
                        0.30,
                        4096,
                        1229,
                        2867,
                        List.of(new NodeServiceStatus("loom-web", "http", "http://loom-web/", "degraded", "expected 200 but got 502", Instant.now())),
                        Instant.now()
                ),
                Instant.now(),
                null,
                null
        ));

        assertThat(updated.status()).isEqualTo(NodeStatus.DEGRADED);
        assertThat(updated.lastError()).contains("loom-web");
    }

    @Test
    void marksNodeOfflineWhenHeartbeatIsOlderThanTimeout() {
        var node = nodeService.registerNode(new NodeRegisterRequest(
                "stale-node",
                NodeType.SERVER,
                "127.0.0.2",
                List.of("integration"),
                List.of("probe:http")
        ));

        var updated = nodeService.heartbeat(node.id(), new NodeHeartbeatRequest(
                node.id(),
                node.name(),
                new NodeSnapshot(
                        "stale-node",
                        "Linux",
                        0.10,
                        0.05,
                        0.20,
                        1024,
                        205,
                        819,
                        0.15,
                        2048,
                        307,
                        1741,
                        List.of(new NodeServiceStatus("loom-server", "http", "http://loom-server:8080/api/health", "online", "ok", Instant.now())),
                        Instant.now()
                ),
                Instant.now().minusSeconds(300),
                null,
                null
        ));

        assertThat(updated.status()).isEqualTo(NodeStatus.OFFLINE);
    }

    @Test
    void keepsLastReportedErrorEvenWhenSnapshotLooksHealthy() {
        var node = nodeService.registerNode(new NodeRegisterRequest(
                "error-node",
                NodeType.SERVER,
                "127.0.0.3",
                List.of("integration"),
                List.of("probe:http")
        ));

        var updated = nodeService.heartbeat(node.id(), new NodeHeartbeatRequest(
                node.id(),
                node.name(),
                new NodeSnapshot(
                        "error-node",
                        "Linux",
                        0.12,
                        0.04,
                        0.22,
                        1024,
                        225,
                        799,
                        0.18,
                        2048,
                        369,
                        1679,
                        List.of(new NodeServiceStatus("loom-server", "http", "http://loom-server:8080/api/health", "online", "ok", Instant.now())),
                        Instant.now()
                ),
                Instant.now(),
                null,
                "node gateway timeout"
        ));

        assertThat(updated.status()).isEqualTo(NodeStatus.ONLINE);
        assertThat(updated.lastError()).isEqualTo("node gateway timeout");
    }
}
