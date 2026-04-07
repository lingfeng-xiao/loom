package com.loom.server;

import static org.assertj.core.api.Assertions.assertThat;

import com.loom.server.dto.ConversationCreateRequest;
import com.loom.server.dto.MemoryCreateRequest;
import com.loom.server.dto.MessageCreateRequest;
import com.loom.server.dto.NodeHeartbeatRequest;
import com.loom.server.dto.NodeRegisterRequest;
import com.loom.server.dto.ProjectCreateRequest;
import com.loom.server.dto.WorkspaceSettingsUpdateRequest;
import com.loom.server.model.AssetType;
import com.loom.server.model.CommandId;
import com.loom.server.model.ConversationMode;
import com.loom.server.model.MemoryScope;
import com.loom.server.model.NodeServiceStatus;
import com.loom.server.model.NodeSnapshot;
import com.loom.server.model.NodeStatus;
import com.loom.server.model.NodeType;
import com.loom.server.model.ProjectType;
import com.loom.server.model.SkillId;
import com.loom.server.service.AssetService;
import com.loom.server.service.ConversationService;
import com.loom.server.service.MemoryService;
import com.loom.server.service.NodeService;
import com.loom.server.service.PlanService;
import com.loom.server.service.ProjectService;
import com.loom.server.service.WorkspaceSettingsService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ServerPersistenceIntegrationTest extends AbstractMySqlIntegrationTest {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private MemoryService memoryService;

    @Autowired
    private PlanService planService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private NodeService nodeService;

    @Autowired
    private WorkspaceSettingsService workspaceSettingsService;

    @Test
    void persistsCoreDataAcrossServices() {
        var project = projectService.createProject(new ProjectCreateRequest(
                "db-persistence-test",
                ProjectType.KNOWLEDGE,
                "validate persistence across project, conversation, memory, plan, asset, node, and settings",
                List.of(SkillId.KNOWLEDGE_CARD_GENERATOR),
                List.of(CommandId.PLAN, CommandId.SAVE_CARD),
                List.of(),
                List.of("docs")
        ));

        var conversation = conversationService.createConversation(
                project.id(),
                new ConversationCreateRequest("persistence-conversation", ConversationMode.CHAT, "validate message append")
        );
        var message = conversationService.addMessage(conversation.id(), new MessageCreateRequest("user", "first message"));
        var memory = memoryService.createMemory(new MemoryCreateRequest(
                MemoryScope.PROJECT,
                project.id(),
                "important-memory",
                "must be persisted",
                90,
                "test",
                message.id()
        ));
        var plan = planService.createPlan(project.id(), conversation.id(), "persist data to mysql", Map.of());
        planService.approvePlan(plan.id(), "test");
        var completedPlan = planService.completePlan(plan.id(), "plan completed", List.of(), List.of("done"));
        var asset = assetService.createManualAsset(project.id(), AssetType.KNOWLEDGE_CARD, "persisted-asset", "asset body", List.of("test"));

        var node = nodeService.registerNode(new NodeRegisterRequest(
                "integration-node",
                NodeType.SERVER,
                "127.0.0.1",
                List.of("integration"),
                List.of("probe:http")
        ));
        var heartbeat = nodeService.heartbeat(node.id(), new NodeHeartbeatRequest(
                node.id(),
                node.name(),
                new NodeSnapshot(
                        "integration-node",
                        "Linux",
                        0.25,
                        0.10,
                        0.50,
                        1024,
                        512,
                        512,
                        0.30,
                        2048,
                        614,
                        1434,
                        List.of(new NodeServiceStatus(
                                "loom-server",
                                "http",
                                "http://127.0.0.1:8080/api/health",
                                "online",
                                "ok",
                                Instant.now()
                        )),
                        Instant.now()
                ),
                Instant.now(),
                null,
                null
        ));

        var settings = workspaceSettingsService.updateSettings(new WorkspaceSettingsUpdateRequest(
                "Loom Test",
                null,
                null,
                project.id(),
                "project_home",
                false,
                null,
                null,
                null,
                null,
                null,
                null
        ));

        assertThat(projectService.listProjects()).extracting("id").contains(project.id());
        assertThat(conversationService.listConversations(project.id())).extracting("id").contains(conversation.id());
        assertThat(conversationService.listMessages(conversation.id())).extracting("id").contains(message.id());
        assertThat(memoryService.listMemories(project.id(), null)).extracting("id").contains(memory.id());
        assertThat(planService.listPlans()).extracting("id").contains(completedPlan.id());
        assertThat(assetService.listAssetsByProject(project.id())).extracting("id").contains(asset.id());
        assertThat(nodeService.listNodes()).extracting("id").contains(node.id());
        assertThat(heartbeat.status()).isEqualTo(NodeStatus.ONLINE);
        assertThat(settings.defaultProjectId()).isEqualTo(project.id());
        assertThat(projectService.dashboard(project.id()).project().id()).isEqualTo(project.id());
    }
}
