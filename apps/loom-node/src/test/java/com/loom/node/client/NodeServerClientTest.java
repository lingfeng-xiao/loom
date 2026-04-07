package com.loom.node.client;

import com.loom.node.config.NodeProperties;
import com.loom.node.dto.NodeHeartbeatRequest;
import com.loom.node.dto.NodeSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

class NodeServerClientTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private NodeProperties properties;
    private NodeServerClient client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);

        properties = new NodeProperties();
        properties.setName("loom-node");
        properties.setType("server");
        properties.setHost("localhost");
        properties.setServerBaseUrl("http://example.test");
        properties.setServerToken("secret");

        client = new NodeServerClient(restTemplate);
    }

    @Test
    void registerSendsBearerTokenAndParsesResponse() {
        server.expect(requestTo("http://example.test/api/nodes/register"))
                .andExpect(method(POST))
                .andExpect(header("Authorization", "Bearer secret"))
                .andRespond(withSuccess("""
                        {"nodeId":"node-1","status":"registered","message":"ok"}
                        """, MediaType.APPLICATION_JSON));

        var response = client.register(properties, new com.loom.node.dto.NodeRegistrationRequest(
                "loom-node",
                "server",
                "localhost",
                List.of("local"),
                List.of("loom-server"),
                List.of("loom-server", "loom-web")
        ));

        server.verify();
        assertThat(response.nodeId()).isEqualTo("node-1");
        assertThat(response.status()).isEqualTo("registered");
    }

    @Test
    void heartbeatSendsPayload() {
        server.expect(requestTo("http://example.test/api/nodes/node-1/heartbeat"))
                .andExpect(method(POST))
                .andExpect(header("Authorization", "Bearer secret"))
                .andRespond(withSuccess("""
                        {"status":"acknowledged","message":"ok"}
                        """, MediaType.APPLICATION_JSON));

        var snapshot = new NodeSnapshot("host", "Linux", 1.0, 1.0, 1024, 512, 512, 25.0, List.of(), Instant.now());
        var response = client.heartbeat(
                properties,
                "node-1",
                new NodeHeartbeatRequest("node-1", "loom-node", snapshot, Instant.now(), "recent probe timeout")
        );

        server.verify();
        assertThat(response.nodeId()).isEqualTo("node-1");
        assertThat(response.status()).isEqualTo("acknowledged");
    }

    @Test
    void surfacesGatewayErrorsWithStatusAndBody() {
        server.expect(requestTo("http://example.test/api/nodes/register"))
                .andExpect(method(POST))
                .andRespond(withStatus(UNAUTHORIZED).body("""
                        {"message":"unauthorized"}
                        """).contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.register(properties, new com.loom.node.dto.NodeRegistrationRequest(
                "loom-node",
                "server",
                "localhost",
                List.of("local"),
                List.of("loom-server"),
                List.of("loom-server", "loom-web")
        )))
                .isInstanceOf(NodeGatewayException.class)
                .satisfies(error -> {
                    NodeGatewayException ex = (NodeGatewayException) error;
                    assertThat(ex.getStatusCode()).isEqualTo(401);
                    assertThat(ex.getResponseBody()).contains("unauthorized");
                });
    }
}
