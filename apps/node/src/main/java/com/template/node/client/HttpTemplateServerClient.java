package com.template.node.client;

import com.template.node.config.NodeProperties;
import com.template.node.dto.NodeHeartbeatRequest;
import com.template.node.dto.NodeHeartbeatResponse;
import com.template.node.dto.NodeRegistrationRequest;
import com.template.node.dto.NodeRegistrationResponse;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class HttpTemplateServerClient implements TemplateServerGateway {

    private final RestTemplate restTemplate;
    private final NodeProperties nodeProperties;

    public HttpTemplateServerClient(RestTemplateBuilder restTemplateBuilder, NodeProperties nodeProperties) {
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
        headers.set("X-Template-Node-Token", nodeProperties.getServerToken());
        return headers;
    }

    public record ApiEnvelope<T>(T data) {
    }
}
