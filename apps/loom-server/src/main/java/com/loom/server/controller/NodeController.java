package com.loom.server.controller;

import com.loom.server.api.ApiEnvelope;
import com.loom.server.dto.NodeHeartbeatRequest;
import com.loom.server.dto.NodeRegisterRequest;
import com.loom.server.dto.NodeSnapshotRequest;
import com.loom.server.model.Node;
import com.loom.server.service.NodeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/nodes")
public class NodeController {

    private final NodeService nodeService;

    public NodeController(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    @GetMapping
    public ApiEnvelope<List<Node>> list() {
        return ApiEnvelope.of(nodeService.listNodes());
    }

    @GetMapping("/{nodeId}")
    public ApiEnvelope<Node> get(@PathVariable String nodeId) {
        return ApiEnvelope.of(nodeService.getNode(nodeId));
    }

    @PostMapping("/register")
    public ApiEnvelope<Node> register(@Valid @RequestBody NodeRegisterRequest request) {
        return ApiEnvelope.of(nodeService.registerNode(request));
    }

    @PostMapping("/{nodeId}/heartbeat")
    public ApiEnvelope<Node> heartbeat(@PathVariable String nodeId, @RequestBody NodeHeartbeatRequest request) {
        return ApiEnvelope.of(nodeService.heartbeat(nodeId, request));
    }

    @PostMapping("/{nodeId}/snapshot")
    public ApiEnvelope<Node> snapshot(@PathVariable String nodeId, @Valid @RequestBody NodeSnapshotRequest request) {
        return ApiEnvelope.of(nodeService.snapshot(nodeId, request));
    }
}
