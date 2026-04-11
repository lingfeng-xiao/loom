package com.loom.node.client;

import com.loom.node.config.NodeProperties;
import com.loom.node.dto.NodeHeartbeatRequest;
import com.loom.node.dto.NodeHeartbeatResponse;
import com.loom.node.dto.NodeRegistrationRequest;
import com.loom.node.dto.NodeRegistrationResponse;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class HttpLoomServerClient implements LoomServerGateway {

    private final RestTemplate restTemplate;
    private final NodeProperties nodeProperties;

    public HttpLoomServerClient(RestTemplateBuilder restTemplateBuilder, NodeProperties nodeProperties) {
        this.restTemplate = restTemplateBuilder.build();
        this.nodeProperties = nodeProperties;
    }

    @Override
    public NodeRegistrationResponse register(NodeRegistrationRequest request) {
        ResponseEntity<ApiEnvelope<NodeRegistrationResponse>> response = restTemplate.exchange(
                nodeProperties.getServerBaseUrl() + "/api/nodes/register",
                org.springframework.http.HttpMethod.POST,
                new HttpEntity<>(request, headers()),
                new ParameterizedTypeReference<>() {}
        );
        return response.getBody().data();
    }

    @Override
    public NodeHeartbeatResponse heartbeat(String nodeId, NodeHeartbeatRequest request) {
        ResponseEntity<ApiEnvelope<NodeHeartbeatResponse>> response = restTemplate.exchange(
                nodeProperties.getServerBaseUrl() + "/api/nodes/" + nodeId + "/heartbeat",
                org.springframework.http.HttpMethod.POST,
                new HttpEntity<>(request, headers()),
                new ParameterizedTypeReference<>() {}
        );
        return response.getBody().data();
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Loom-Node-Token", nodeProperties.getServerToken());
        return headers;
    }

    public record ApiEnvelope<T>(T data) {
    }
}
