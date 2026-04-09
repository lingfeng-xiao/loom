package com.loom.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LoomApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void healthAndBootstrapEndpointsReturnLoomShellMetadata() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ok"));

        mockMvc.perform(get("/api/bootstrap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appName").value("loom"))
                .andExpect(jsonPath("$.data.project.name").isNotEmpty())
                .andExpect(jsonPath("$.data.pages[0].id").value("conversation"))
                .andExpect(jsonPath("$.data.pages[3].id").value("files"))
                .andExpect(jsonPath("$.data.pages[3].available").value(true))
                .andExpect(jsonPath("$.data.traceSteps[2].status").isNotEmpty())
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
                .andExpect(jsonPath("$.data.activeGoals[0]").value("Start from the correct project."));

        mockMvc.perform(post("/api/projects/project-loom/conversations/conversation-v1/context/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.context.activeGoals[1]").value("Refresh #1"))
                .andExpect(jsonPath("$.data.context.references[1].kind").value("conversation"));

        mockMvc.perform(get("/api/projects/project-loom/conversations/conversation-v1/trace"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.steps[0].title").value("Load context"));

        mockMvc.perform(get("/api/settings/overview").queryParam("scope", "project"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tabs[0]").value("Models"))
                .andExpect(jsonPath("$.data.modelProfiles[0].name").value("MiniMax M2.7 (setup required)"))
                .andExpect(jsonPath("$.data.providerPresets[0].id").value("minimax-cn"))
                .andExpect(jsonPath("$.data.providerPresets[0].modelOptions[0].id").value("MiniMax-M2.7"))
                .andExpect(jsonPath("$.data.activeLlmConfig.provider").value("MiniMax"));

        mockMvc.perform(get("/api/capabilities/overview").queryParam("scope", "project"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeScope").value("project"))
                .andExpect(jsonPath("$.data.cards[0].title").value("Models"))
                .andExpect(jsonPath("$.data.bindingRules[0].label").value("Default chat model"));

        mockMvc.perform(get("/api/projects/project-loom/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].displayName").value("loom-prd.md"))
                .andExpect(jsonPath("$.data.items[2].parseStatus").value("pending"));

        mockMvc.perform(get("/api/projects/project-loom/memory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].scope").value("project"))
                .andExpect(jsonPath("$.data.hasMore").value(false));
    }

    @Test
    void messageSubmissionUpdatesConversationAndReturnsStreamPath() throws Exception {
        String response = mockMvc.perform(post("/api/projects/project-loom/conversations/conversation-v1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "body": "请继续推进真实接口和前端接线"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conversationId").value("conversation-v1"))
                .andExpect(jsonPath("$.data.acceptedRunId").exists())
                .andExpect(jsonPath("$.data.streamPath").value("/api/projects/project-loom/conversations/conversation-v1/stream"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode submitPayload = objectMapper.readTree(response).path("data");
        String runId = submitPayload.path("acceptedRunId").asText();

        mockMvc.perform(get("/api/projects/project-loom/conversations/conversation-v1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeRunId").exists())
                .andExpect(jsonPath("$.data.status").value("active"))
                .andExpect(jsonPath("$.data.contextSummary").value("The latest request was accepted and the reply is now streaming."));

        mockMvc.perform(get("/api/projects/project-loom/conversations/conversation-v1/runs/" + runId + "/steps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].runId").value(runId))
                .andExpect(jsonPath("$.data.items[1].title").value("Call model"));

        String traceResponse = mockMvc.perform(get("/api/projects/project-loom/conversations/conversation-v1/trace"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeAction.id").exists())
                .andExpect(jsonPath("$.data.activeAction.runId").value(runId))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode tracePayload = objectMapper.readTree(traceResponse).path("data");
        String actionId = tracePayload.path("activeAction").path("id").asText();

        mockMvc.perform(get("/api/projects/project-loom/conversations/conversation-v1/actions/" + actionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(actionId))
                .andExpect(jsonPath("$.data.runId").value(runId))
                .andExpect(jsonPath("$.data.status").value("running"));

        mockMvc.perform(get("/api/projects/project-loom/conversations/conversation-v1/stream"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("event:thinking.summary.delta")))
                .andExpect(content().string(containsString("event:thinking.summary.done")))
                .andExpect(content().string(containsString("event:message.delta")))
                .andExpect(content().string(containsString("event:message.done")))
                .andExpect(content().string(containsString("event:trace.step.created")))
                .andExpect(content().string(containsString("event:trace.step.completed")))
                .andExpect(content().string(containsString("trace-publish-conversation-v1")));

        mockMvc.perform(get("/api/projects/project-loom/conversations/conversation-v1/context"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conversationSummary").isNotEmpty());
    }

    @Test
    void projectCreationAndLlmSettingsUpdateAreAvailable() throws Exception {
        String createProjectResponse = mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value(org.hamcrest.Matchers.startsWith("Project ")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode createdProject = objectMapper.readTree(createProjectResponse).path("data");
        org.junit.jupiter.api.Assertions.assertTrue(createdProject.path("id").asText().startsWith("project-"));

        mockMvc.perform(post("/api/settings/llm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "presetId": "minimax-cn",
                                  "modelId": "MiniMax-M2.7",
                                  "apiKey": "sk-test-minimax-12345678"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeLlmConfig.configured").value(true))
                .andExpect(jsonPath("$.data.activeLlmConfig.apiKeyHint").value("sk-t...5678"))
                .andExpect(jsonPath("$.data.modelProfiles[0].name").value("MiniMax (China)"));
    }

    @Test
    void contextSnapshotsAndMemoryLifecycleEndpointsWork() throws Exception {
        mockMvc.perform(post("/api/projects/project-loom/conversations/conversation-v1/context/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.context.activeGoals[1]").value("Refresh #1"));

        mockMvc.perform(get("/api/projects/project-loom/conversations/conversation-v1/context/snapshots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].conversationId").value("conversation-v1"))
                .andExpect(jsonPath("$.data.items[0].kind").isNotEmpty());

        String createMemoryResponse = mockMvc.perform(post("/api/projects/project-loom/memory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "conversationId": "conversation-v1",
                                  "scope": "conversation",
                                  "content": "The team prefers project-first session creation.",
                                  "source": "explicit"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scope").value("conversation"))
                .andExpect(jsonPath("$.data.content").value("The team prefers project-first session creation."))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String memoryId = objectMapper.readTree(createMemoryResponse).path("data").path("id").asText();

        mockMvc.perform(get("/api/projects/project-loom/memory")
                        .queryParam("scope", "conversation")
                        .queryParam("conversationId", "conversation-v1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(memoryId));

        mockMvc.perform(patch("/api/projects/project-loom/memory/" + memoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "conversationId": "conversation-v1",
                                  "scope": "conversation",
                                  "content": "Project-first session creation is a standing preference.",
                                  "source": "explicit"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("Project-first session creation is a standing preference."));

        String createSuggestionResponse = mockMvc.perform(post("/api/projects/project-loom/memory/suggestions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "conversationId": "conversation-v1",
                                  "scope": "conversation",
                                  "content": "The active thread should preserve the long-term preference."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String suggestionId = objectMapper.readTree(createSuggestionResponse).path("data").path("id").asText();

        String listedSuggestionsResponse = mockMvc.perform(get("/api/projects/project-loom/memory/suggestions")
                        .queryParam("conversationId", "conversation-v1")
                        .queryParam("scope", "conversation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].status").value("pending"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode listedSuggestions = objectMapper.readTree(listedSuggestionsResponse).path("data").path("items");
        org.junit.jupiter.api.Assertions.assertTrue(
                listedSuggestions.isArray()
                        && java.util.stream.StreamSupport.stream(listedSuggestions.spliterator(), false)
                        .anyMatch(item -> suggestionId.equals(item.path("id").asText()) && "pending".equals(item.path("status").asText()))
        );

        mockMvc.perform(post("/api/projects/project-loom/memory/suggestions/" + suggestionId + "/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("accepted"));

        mockMvc.perform(delete("/api/projects/project-loom/memory/" + memoryId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/projects/project-loom/memory")
                        .queryParam("scope", "conversation")
                        .queryParam("conversationId", "conversation-v1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id", startsWith("memory-item-")));
    }

    @Test
    void conversationCanMoveToAnotherProject() throws Exception {
        String createProjectResponse = mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value(org.hamcrest.Matchers.startsWith("Project ")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String movedProjectId = objectMapper.readTree(createProjectResponse).path("data").path("id").asText();

        mockMvc.perform(patch("/api/projects/project-loom/conversations/conversation-shell")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId": "%s"
                                }
                                """.formatted(movedProjectId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projectId").value(movedProjectId));

        mockMvc.perform(get("/api/projects/" + movedProjectId + "/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value("conversation-shell"));
    }

    @Test
    void actionLookupReturnsNotFoundForUnknownAction() throws Exception {
        mockMvc.perform(get("/api/projects/project-loom/conversations/conversation-v1/actions/action-does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ACTION_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("Action does not exist"));
    }
}
