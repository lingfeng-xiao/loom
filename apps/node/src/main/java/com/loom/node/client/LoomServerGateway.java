package com.loom.node.client;

import com.loom.node.dto.NodeHeartbeatRequest;
import com.loom.node.dto.NodeHeartbeatResponse;
import com.loom.node.dto.NodeRegistrationRequest;
import com.loom.node.dto.NodeRegistrationResponse;

public interface LoomServerGateway {

    NodeRegistrationResponse register(NodeRegistrationRequest request);

    NodeHeartbeatResponse heartbeat(String nodeId, NodeHeartbeatRequest request);
}
