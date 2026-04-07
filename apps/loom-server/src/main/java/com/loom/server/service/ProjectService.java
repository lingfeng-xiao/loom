package com.loom.server.service;

import com.loom.server.dto.ProjectCreateRequest;
import com.loom.server.dto.ProjectUpdateRequest;
import com.loom.server.error.NotFoundException;
import com.loom.server.model.Asset;
import com.loom.server.model.Conversation;
import com.loom.server.model.Memory;
import com.loom.server.model.Node;
import com.loom.server.model.Plan;
import com.loom.server.model.PlanStatus;
import com.loom.server.model.Project;
import com.loom.server.model.ProjectType;
import com.loom.server.model.WorkspaceSettings;
import com.loom.server.repository.AssetRepository;
import com.loom.server.repository.ConversationRepository;
import com.loom.server.repository.MemoryRepository;
import com.loom.server.repository.NodeRepository;
import com.loom.server.repository.PlanRepository;
import com.loom.server.repository.ProjectRepository;
import com.loom.server.repository.WorkspaceSettingsRepository;
import com.loom.server.support.Responses.DashboardResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ConversationRepository conversationRepository;
    private final MemoryRepository memoryRepository;
    private final AssetRepository assetRepository;
    private final NodeRepository nodeRepository;
    private final PlanRepository planRepository;
    private final WorkspaceSettingsRepository workspaceSettingsRepository;
    private final AuditService auditService;

    public ProjectService(
            ProjectRepository projectRepository,
            ConversationRepository conversationRepository,
            MemoryRepository memoryRepository,
            AssetRepository assetRepository,
            NodeRepository nodeRepository,
            PlanRepository planRepository,
            WorkspaceSettingsRepository workspaceSettingsRepository,
            AuditService auditService
    ) {
        this.projectRepository = projectRepository;
        this.conversationRepository = conversationRepository;
        this.memoryRepository = memoryRepository;
        this.assetRepository = assetRepository;
        this.nodeRepository = nodeRepository;
        this.planRepository = planRepository;
        this.workspaceSettingsRepository = workspaceSettingsRepository;
        this.auditService = auditService;
    }

    public List<Project> listProjects() {
        return projectRepository.findAll();
    }

    public Project getProject(String id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("未找到项目 " + id));
    }

    public Project createProject(ProjectCreateRequest request) {
        Instant now = Instant.now();
        Project project = new Project(
                UUID.randomUUID().toString(),
                request.name(),
                request.type(),
                request.description() == null ? "" : request.description(),
                request.defaultSkills() == null ? List.of() : List.copyOf(request.defaultSkills()),
                request.defaultCommands() == null ? List.of() : List.copyOf(request.defaultCommands()),
                request.boundNodeIds() == null ? List.of() : List.copyOf(request.boundNodeIds()),
                request.knowledgeRoots() == null ? List.of() : List.copyOf(request.knowledgeRoots()),
                new ArrayList<>(),
                now,
                now
        );
        projectRepository.save(project);
        maybePersistDefaultProject(project.id());
        auditService.record("system", "project-service", "project", project.id(), "create", project.name(), "ok");
        return project;
    }

    public Project createProject(String name, Map<String, String> args) {
        ProjectType type = ProjectType.KNOWLEDGE;
        if (args != null && args.containsKey("type")) {
            type = ProjectType.fromValue(args.get("type"));
        }
        ProjectCreateRequest request = new ProjectCreateRequest(
                name,
                type,
                args == null ? "" : args.getOrDefault("description", ""),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        return createProject(request);
    }

    public Project updateProject(String id, ProjectUpdateRequest request) {
        Project current = getProject(id);
        Project updated = new Project(
                current.id(),
                request.name() == null ? current.name() : request.name(),
                request.type() == null ? current.type() : request.type(),
                request.description() == null ? current.description() : request.description(),
                request.defaultSkills() == null ? current.defaultSkills() : List.copyOf(request.defaultSkills()),
                request.defaultCommands() == null ? current.defaultCommands() : List.copyOf(request.defaultCommands()),
                request.boundNodeIds() == null ? current.boundNodeIds() : List.copyOf(request.boundNodeIds()),
                request.knowledgeRoots() == null ? current.knowledgeRoots() : List.copyOf(request.knowledgeRoots()),
                request.projectMemoryRefs() == null ? current.projectMemoryRefs() : List.copyOf(request.projectMemoryRefs()),
                current.createdAt(),
                Instant.now()
        );
        projectRepository.save(updated);
        auditService.record("system", "project-service", "project", id, "update", updated.name(), "ok");
        return updated;
    }

    public Project switchProject(String id) {
        Project project = getProject(id);
        workspaceSettingsRepository.find().ifPresent(current -> workspaceSettingsRepository.save(new WorkspaceSettings(
                current.workspaceName(),
                current.language(),
                current.density(),
                project.id(),
                current.defaultLandingView(),
                current.inspectorDefaultOpen(),
                current.model(),
                current.vault(),
                current.nodes(),
                current.appearance(),
                current.enabledCommands(),
                current.enabledSkills(),
                Instant.now()
        )));
        auditService.record("system", "project-service", "project", id, "switch", project.name(), "ok");
        return project;
    }

    public void linkMemory(String projectId, String memoryId) {
        Project current = getProject(projectId);
        if (current.projectMemoryRefs().contains(memoryId)) {
            return;
        }
        List<String> refs = new ArrayList<>(current.projectMemoryRefs());
        refs.add(memoryId);
        projectRepository.save(new Project(
                current.id(),
                current.name(),
                current.type(),
                current.description(),
                current.defaultSkills(),
                current.defaultCommands(),
                current.boundNodeIds(),
                current.knowledgeRoots(),
                List.copyOf(refs),
                current.createdAt(),
                Instant.now()
        ));
    }

    public DashboardResponse dashboard(String id) {
        Project project = getProject(id);
        List<Conversation> conversations = conversationRepository.findByProject(id, null, null, null);
        List<Memory> memories = memoryRepository.findVisible(id, null);
        List<Asset> recentAssets = assetRepository.findByProject(id).stream().limit(10).toList();
        List<Node> nodes = nodeRepository.findAll().stream()
                .filter(node -> project.boundNodeIds().isEmpty() || project.boundNodeIds().contains(node.id()))
                .sorted(Comparator.comparing(Node::updatedAt).reversed())
                .toList();
        Plan activePlan = planRepository.findByProject(id).stream()
                .filter(plan -> plan.status() == PlanStatus.DRAFT
                        || plan.status() == PlanStatus.READY
                        || plan.status() == PlanStatus.APPROVED
                        || plan.status() == PlanStatus.RUNNING)
                .findFirst()
                .orElse(null);
        return new DashboardResponse(project, conversations, memories, recentAssets, nodes, activePlan);
    }

    public List<com.loom.server.model.AuditLogEntry> recentAuditLogs() {
        return auditService.recent(20);
    }

    private void maybePersistDefaultProject(String projectId) {
        workspaceSettingsRepository.find().ifPresent(current -> {
            if (current.defaultProjectId() == null || current.defaultProjectId().isBlank()) {
                workspaceSettingsRepository.save(new WorkspaceSettings(
                        current.workspaceName(),
                        current.language(),
                        current.density(),
                        projectId,
                        current.defaultLandingView(),
                        current.inspectorDefaultOpen(),
                        current.model(),
                        current.vault(),
                        current.nodes(),
                        current.appearance(),
                        current.enabledCommands(),
                        current.enabledSkills(),
                        Instant.now()
                ));
            }
        });
    }
}
