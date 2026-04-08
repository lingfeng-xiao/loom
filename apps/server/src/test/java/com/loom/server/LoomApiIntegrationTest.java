package com.loom.server;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
                .andExpect(jsonPath("$.data.traceSteps[2].status").value("pending"))
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

    @Test
    void workspaceEndpointsExposePhase1ReadModels() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value("project-loom"));

        mockMvc.perform(get("/api/projects/project-loom/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].projectId").value("project-loom"));

        mockMvc.perform(get("/api/projects/project-loom/conversations/conversation-v1/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].conversationId").value("conversation-v1"));

        mockMvc.perform(get("/api/projects/project-loom/conversations/conversation-v1/context"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeGoals[0]").value("推进真实主链路"));

        mockMvc.perform(get("/api/projects/project-loom/conversations/conversation-v1/trace"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.steps[0].title").value("读取上下文"));

        mockMvc.perform(get("/api/settings/overview").queryParam("scope", "project"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tabs[0]").value("Models"))
                .andExpect(jsonPath("$.data.modelProfiles[0].name").value("GPT-5.4 Thinking"));
    }

    @Test
    void messageSubmissionUpdatesConversationAndReturnsStreamPath() throws Exception {
        mockMvc.perform(post("/api/projects/project-loom/conversations/conversation-v1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "body": "请继续推进真实接口和前端接线"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conversationId").value("conversation-v1"))
                .andExpect(jsonPath("$.data.acceptedRunId").exists())
                .andExpect(jsonPath("$.data.streamPath").value("/api/projects/project-loom/conversations/conversation-v1/stream"));

        mockMvc.perform(get("/api/projects/project-loom/conversations/conversation-v1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeRunId").exists())
                .andExpect(jsonPath("$.data.status").value("active"));

        mockMvc.perform(get("/api/projects/project-loom/conversations/conversation-v1/stream"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("event:thinking.summary.delta")))
                .andExpect(content().string(containsString("event:thinking.summary.done")))
                .andExpect(content().string(containsString("event:message.delta")))
                .andExpect(content().string(containsString("event:message.done")))
                .andExpect(content().string(containsString("event:trace.step.created")))
                .andExpect(content().string(containsString("event:trace.step.completed")))
                .andExpect(content().string(containsString("event:context.updated")))
                .andExpect(content().string(containsString("event:run.completed")));
    }
}
