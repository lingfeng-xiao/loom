package com.loom.server.service;

import com.loom.server.config.LoomServerProperties;
import com.loom.server.dto.NodeHeartbeatRequest;
import com.loom.server.dto.NodeRegistrationRequest;
import com.loom.server.model.NodeRecord;
import com.loom.server.repository.NodeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NodeService {

    private final NodeRepository nodeRepository;
    private final LoomServerProperties serverProperties;

    public NodeService(NodeRepository nodeRepository, LoomServerProperties serverProperties) {
        this.nodeRepository = nodeRepository;
        this.serverProperties = serverProperties;
    }

    public List<NodeRecord> findAll() {
        return nodeRepository.findAll(serverProperties.getNode().getHeartbeatTimeoutSeconds());
    }

    public NodeRecord findById(String nodeId) {
        return nodeRepository.findById(nodeId, serverProperties.getNode().getHeartbeatTimeoutSeconds())
                .orElseThrow(() -> new IllegalArgumentException("Unknown node id: " + nodeId));
    }

    public NodeRecord register(NodeRegistrationRequest request) {
        return nodeRepository.register(request, serverProperties.getNode().getHeartbeatTimeoutSeconds());
    }

    public NodeRecord heartbeat(String nodeId, NodeHeartbeatRequest request) {
        return nodeRepository.heartbeat(nodeId, request, serverProperties.getNode().getHeartbeatTimeoutSeconds());
    }
}
