package com.template.server.controller;

import com.template.server.api.ApiEnvelope;
import com.template.server.config.TemplateProperties;
import com.template.server.dto.NodeHeartbeatRequest;
import com.template.server.dto.NodeHeartbeatResponse;
import com.template.server.dto.NodeRegistrationRequest;
import com.template.server.dto.NodeRegistrationResponse;
import com.template.server.model.NodeRecord;
import com.template.server.service.NodeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@RestController
public class NodeController {

    private final NodeService nodeService;
    private final TemplateProperties templateProperties;

    public NodeController(NodeService nodeService, TemplateProperties templateProperties) {
        this.nodeService = nodeService;
        this.templateProperties = templateProperties;
    }

    @GetMapping("/api/nodes")
    public ApiEnvelope<List<NodeRecord>> listNodes() {
        return ApiEnvelope.of(nodeService.findAll());
    }

    @GetMapping("/api/nodes/{nodeId}")
    public ApiEnvelope<NodeRecord> getNode(@PathVariable String nodeId) {
        return ApiEnvelope.of(nodeService.findById(nodeId));
    }

    @PostMapping("/api/nodes/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiEnvelope<NodeRegistrationResponse> register(
            @RequestHeader("X-Template-Node-Token") String token,
            @Valid @RequestBody NodeRegistrationRequest request
    ) {
        verifyToken(token);
        NodeRecord record = nodeService.register(request);
        return ApiEnvelope.of(new NodeRegistrationResponse(record.id(), Instant.now()));
    }

    @PostMapping("/api/nodes/{nodeId}/heartbeat")
    public ApiEnvelope<NodeHeartbeatResponse> heartbeat(
            @PathVariable String nodeId,
            @RequestHeader("X-Template-Node-Token") String token,
            @Valid @RequestBody NodeHeartbeatRequest request
    ) {
        verifyToken(token);
        nodeService.heartbeat(nodeId, request);
        return ApiEnvelope.of(new NodeHeartbeatResponse(nodeId, Instant.now()));
    }

    private void verifyToken(String token) {
        if (!templateProperties.getNode().getServerToken().equals(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid node token");
        }
    }
}
