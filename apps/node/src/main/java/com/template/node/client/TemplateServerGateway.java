package com.template.node.client;

import com.template.node.dto.NodeHeartbeatRequest;
import com.template.node.dto.NodeHeartbeatResponse;
import com.template.node.dto.NodeRegistrationRequest;
import com.template.node.dto.NodeRegistrationResponse;

public interface TemplateServerGateway {

    NodeRegistrationResponse register(NodeRegistrationRequest request);

    NodeHeartbeatResponse heartbeat(String nodeId, NodeHeartbeatRequest request);
}
