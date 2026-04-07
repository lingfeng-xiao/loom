package com.template.server;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TemplateApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthAndBootstrapEndpointsReturnTemplateMetadata() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ok"));

        mockMvc.perform(get("/api/bootstrap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appName").value("Template Infrastructure Stack"))
                .andExpect(jsonPath("$.data.setupTasks[0].id").value("env"));
    }

    @Test
    void settingsCanBeReadAndUpdated() throws Exception {
        mockMvc.perform(put("/api/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workspaceName": "Acme Template",
                                  "supportEmail": "platform@example.com",
                                  "docsUrl": "https://example.com/docs",
                                  "defaultRefreshIntervalSeconds": 45
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workspaceName").value("Acme Template"));

        mockMvc.perform(get("/api/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.supportEmail").value("platform@example.com"))
                .andExpect(jsonPath("$.data.defaultRefreshIntervalSeconds").value(45));
    }

    @Test
    void nodeRegistrationAndHeartbeatPersistProbeState() throws Exception {
        String nodeId = mockMvc.perform(post("/api/nodes/register")
                        .header("X-Template-Node-Token", "integration-test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "template-node",
                                  "type": "server",
                                  "host": "localhost",
                                  "tags": ["edge", "linux"],
                                  "capabilities": ["http-probe", "scheduler"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll(".*\"nodeId\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(post("/api/nodes/" + nodeId + "/heartbeat")
                        .header("X-Template-Node-Token", "integration-test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "up",
                                  "probes": [
                                    {
                                      "name": "template-server",
                                      "kind": "http",
                                      "target": "http://template-server:8080/api/health",
                                      "status": "up",
                                      "detail": "200 OK"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nodeId").value(nodeId));

        mockMvc.perform(get("/api/nodes/" + nodeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("up"))
                .andExpect(jsonPath("$.data.probes[0].name").value("template-server"));
    }
}
