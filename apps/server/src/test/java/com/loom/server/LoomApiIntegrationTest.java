package com.loom.server;

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
class LoomApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthAndBootstrapEndpointsReturnLoomShellMetadata() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ok"));

        mockMvc.perform(get("/api/bootstrap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appName").value("loom"))
                .andExpect(jsonPath("$.data.project.name").value("loom"))
                .andExpect(jsonPath("$.data.pages[0].id").value("conversation"))
                .andExpect(jsonPath("$.data.pages[3].id").value("files"))
                .andExpect(jsonPath("$.data.pages[3].available").value(false))
                .andExpect(jsonPath("$.data.messages[1].kind").value("thinking_summary"))
                .andExpect(jsonPath("$.data.traceSteps[2].status").value("running"))
                .andExpect(jsonPath("$.data.settings.tabs[0]").value("Models"));
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
