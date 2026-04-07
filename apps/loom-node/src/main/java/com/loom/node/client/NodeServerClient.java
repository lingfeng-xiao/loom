package com.loom.node.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loom.node.config.NodeProperties;
import com.loom.node.dto.NodeHeartbeatRequest;
import com.loom.node.dto.NodeHeartbeatResponse;
import com.loom.node.dto.NodeRegistrationRequest;
import com.loom.node.dto.NodeRegistrationResponse;
import java.net.URI;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class NodeServerClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public NodeServerClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public NodeRegistrationResponse register(NodeProperties properties, NodeRegistrationRequest request) {
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    URI.create(properties.getServerBaseUrl() + "/api/nodes/register"),
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers(properties)),
                    String.class
            );
            return parseRegistrationResponse(response.getBody(), request);
        } catch (HttpStatusCodeException ex) {
            throw new NodeGatewayException(
                    ex.getStatusCode().value(),
                    ex.getResponseBodyAsString(),
                    "Failed to register node: " + ex.getStatusCode().value(),
                    ex
            );
        } catch (RestClientException ex) {
            throw new NodeGatewayException("Failed to register node: " + ex.getMessage(), ex);
        }
    }

    public NodeHeartbeatResponse heartbeat(NodeProperties properties, String nodeId, NodeHeartbeatRequest request) {
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    URI.create(properties.getServerBaseUrl() + "/api/nodes/" + nodeId + "/heartbeat"),
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers(properties)),
                    String.class
            );
            return parseHeartbeatResponse(response.getBody(), nodeId);
        } catch (HttpStatusCodeException ex) {
            throw new NodeGatewayException(
                    ex.getStatusCode().value(),
                    ex.getResponseBodyAsString(),
                    "Failed to send heartbeat: " + ex.getStatusCode().value(),
                    ex
            );
        } catch (RestClientException ex) {
            throw new NodeGatewayException("Failed to send heartbeat: " + ex.getMessage(), ex);
        }
    }

    private HttpHeaders headers(NodeProperties properties) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getServerToken());
        return headers;
    }

    private NodeRegistrationResponse parseRegistrationResponse(String body, NodeRegistrationRequest request) {
        if (body == null || body.isBlank()) {
            return new NodeRegistrationResponse(request.name(), "registered", "no body returned");
        }
        try {
            JsonNode json = objectMapper.readTree(body);
            String nodeId = textOrDefault(json, "nodeId", textOrDefault(json, "id", request.name()));
            String status = textOrDefault(json, "status", "registered");
            String message = textOrDefault(json, "message", "ok");
            return new NodeRegistrationResponse(nodeId, status, message);
        } catch (Exception ex) {
            return new NodeRegistrationResponse(request.name(), "registered", "unparsed response");
        }
    }

    private NodeHeartbeatResponse parseHeartbeatResponse(String body, String nodeId) {
        if (body == null || body.isBlank()) {
            return new NodeHeartbeatResponse(nodeId, "acknowledged", "no body returned");
        }
        try {
            JsonNode json = objectMapper.readTree(body);
            String status = textOrDefault(json, "status", "acknowledged");
            String message = textOrDefault(json, "message", "ok");
            return new NodeHeartbeatResponse(nodeId, status, message);
        } catch (Exception ex) {
            return new NodeHeartbeatResponse(nodeId, "acknowledged", "unparsed response");
        }
    }

    private String textOrDefault(JsonNode json, String key, String defaultValue) {
        JsonNode value = json.get(key);
        if (value == null || value.isNull()) {
            JsonNode data = json.get("data");
            if (data != null && data.isObject()) {
                JsonNode nested = data.get(key);
                if (nested != null && nested.isTextual()) {
                    return nested.asText();
                }
            }
            return defaultValue;
        }
        return value.asText(defaultValue);
    }
}
