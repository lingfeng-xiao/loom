package com.template.server.service;

import com.template.server.config.TemplateProperties;
import com.template.server.dto.NodeHeartbeatRequest;
import com.template.server.dto.NodeRegistrationRequest;
import com.template.server.model.NodeRecord;
import com.template.server.repository.NodeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NodeService {

    private final NodeRepository nodeRepository;
    private final TemplateProperties templateProperties;

    public NodeService(NodeRepository nodeRepository, TemplateProperties templateProperties) {
        this.nodeRepository = nodeRepository;
        this.templateProperties = templateProperties;
    }

    public List<NodeRecord> findAll() {
        return nodeRepository.findAll(templateProperties.getNode().getHeartbeatTimeoutSeconds());
    }

    public NodeRecord findById(String nodeId) {
        return nodeRepository.findById(nodeId, templateProperties.getNode().getHeartbeatTimeoutSeconds())
                .orElseThrow(() -> new IllegalArgumentException("Unknown node id: " + nodeId));
    }

    public NodeRecord register(NodeRegistrationRequest request) {
        return nodeRepository.register(request, templateProperties.getNode().getHeartbeatTimeoutSeconds());
    }

    public NodeRecord heartbeat(String nodeId, NodeHeartbeatRequest request) {
        return nodeRepository.heartbeat(nodeId, request, templateProperties.getNode().getHeartbeatTimeoutSeconds());
    }
}
