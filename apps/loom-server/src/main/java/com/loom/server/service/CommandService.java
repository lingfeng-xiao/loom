package com.loom.server.service;

import com.loom.server.dto.AssetFromConversationRequest;
import com.loom.server.dto.AssetFromPlanRequest;
import com.loom.server.dto.CommandExecuteRequest;
import com.loom.server.dto.MemoryCreateRequest;
import com.loom.server.error.BadRequestException;
import com.loom.server.model.Asset;
import com.loom.server.model.CommandId;
import com.loom.server.model.Conversation;
import com.loom.server.model.Memory;
import com.loom.server.model.Node;
import com.loom.server.model.Plan;
import com.loom.server.model.Project;
import com.loom.server.model.Skill;
import com.loom.server.support.Responses.CommandExecutionResult;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CommandService {

    private final ProjectService projectService;
    private final ConversationService conversationService;
    private final MemoryService memoryService;
    private final PlanService planService;
    private final SkillService skillService;
    private final AssetService assetService;
    private final NodeService nodeService;
    private final AuditService auditService;

    public CommandService(
            ProjectService projectService,
            ConversationService conversationService,
            MemoryService memoryService,
            PlanService planService,
            SkillService skillService,
            AssetService assetService,
            NodeService nodeService,
            AuditService auditService
    ) {
        this.projectService = projectService;
        this.conversationService = conversationService;
        this.memoryService = memoryService;
        this.planService = planService;
        this.skillService = skillService;
        this.assetService = assetService;
        this.nodeService = nodeService;
        this.auditService = auditService;
    }

    public List<CommandId> listCommands() {
        return List.of(CommandId.values());
    }

    public CommandExecutionResult execute(CommandExecuteRequest request) {
        return switch (request.commandId()) {
            case PROJECT_NEW -> projectNew(request);
            case PROJECT_SWITCH -> projectSwitch(request);
            case PROJECT_STATUS -> projectStatus(request);
            case PLAN -> plan(request);
            case PLAN_RUN -> planRun(request);
            case SAVE_CARD -> saveCard(request);
            case MEMORY_SHOW -> memoryShow(request);
            case MEMORY_SAVE -> memorySave(request);
            case SKILL_LIST -> skillList(request);
            case NODE_STATUS -> nodeStatus(request);
            case LOGS -> logs(request);
        };
    }

    private CommandExecutionResult projectNew(CommandExecuteRequest request) {
        String name = request.args() == null ? "未命名项目" : request.args().getOrDefault("name", "未命名项目");
        Project project = projectService.createProject(name, request.args());
        return auditAndReturn(request, "项目已创建", project.id(), null, null, null, null, List.of(project), null, null, null, null, null);
    }

    private CommandExecutionResult projectSwitch(CommandExecuteRequest request) {
        String targetProjectId = requireArg(request, "projectId");
        Project project = projectService.switchProject(targetProjectId);
        return auditAndReturn(request, "项目已切换", project.id(), null, null, null, null, List.of(project), null, null, null, null, null);
    }

    private CommandExecutionResult projectStatus(CommandExecuteRequest request) {
        Project project = projectService.getProject(request.projectId());
        return auditAndReturn(
                request,
                "项目状态已就绪",
                project.id(),
                null,
                null,
                null,
                null,
                List.of(project),
                conversationService.listConversations(project.id()),
                memoryService.listMemories(project.id(), null),
                assetService.listAssetsByProject(project.id()),
                skillService.listSkills(),
                nodeService.listNodes()
        );
    }

    private CommandExecutionResult plan(CommandExecuteRequest request) {
        String conversationId = requireArg(request, "conversationId");
        String goal = requireArg(request, "goal");
        Plan plan = planService.createPlan(request.projectId(), conversationId, goal, request.args());
        return auditAndReturn(request, "计划草案已生成", request.projectId(), conversationId, plan.id(), null, null, null, null, null, null, null, null);
    }

    private CommandExecutionResult planRun(CommandExecuteRequest request) {
        String planId = requireArg(request, "planId");
        Plan approved = planService.approvePlan(planId, "command");
        Plan running = planService.runPlan(approved.id());
        Plan completed = planService.completePlan(running.id(), "命令执行已完成", List.of(), List.of("通过 /plan-run 执行"));
        return auditAndReturn(request, "计划已执行", request.projectId(), request.conversationId(), completed.id(), null, null, null, null, null, null, null, null);
    }

    private CommandExecutionResult saveCard(CommandExecuteRequest request) {
        String content = requireArg(request, "content");
        var args = safeArgs(request);
        String title = args.getOrDefault("title", "知识卡片");
        Asset asset;
        if (args.containsKey("planId")) {
            asset = assetService.createFromPlan(new AssetFromPlanRequest(args.get("planId"), title, content, tags(request)));
        } else if (args.containsKey("conversationId")) {
            asset = assetService.createFromConversation(new AssetFromConversationRequest(args.get("conversationId"), title, content, tags(request)));
        } else {
            throw new BadRequestException("/save-card 需要 planId 或 conversationId");
        }
        return auditAndReturn(request, "资产已保存", request.projectId(), request.conversationId(), null, asset.id(), null, null, null, null, List.of(asset), null, null);
    }

    private CommandExecutionResult memoryShow(CommandExecuteRequest request) {
        List<Memory> memories = memoryService.listMemories(request.projectId(), request.args() == null ? null : request.args().get("scope"));
        return auditAndReturn(request, "记忆列表已就绪", request.projectId(), request.conversationId(), null, null, null, null, null, memories, null, null, null);
    }

    private CommandExecutionResult memorySave(CommandExecuteRequest request) {
        String title = requireArg(request, "title");
        String content = requireArg(request, "content");
        var args = safeArgs(request);
        String scope = args.getOrDefault("scope", "project");
        Memory memory = memoryService.createMemoryFromCommand(request.projectId(), scope, title, content, args);
        return auditAndReturn(request, "记忆已保存", request.projectId(), request.conversationId(), null, null, List.of(memory.id()), null, null, List.of(memory), null, null, null);
    }

    private CommandExecutionResult skillList(CommandExecuteRequest request) {
        return auditAndReturn(request, "技能列表已就绪", request.projectId(), request.conversationId(), null, null, null, null, null, null, null, skillService.listSkills(), null);
    }

    private CommandExecutionResult nodeStatus(CommandExecuteRequest request) {
        List<Node> nodes = nodeService.listNodes();
        return auditAndReturn(request, "节点状态已就绪", request.projectId(), request.conversationId(), null, null, null, null, null, null, null, null, nodes);
    }

    private CommandExecutionResult logs(CommandExecuteRequest request) {
        return auditAndReturn(request, "最近日志已就绪", request.projectId(), request.conversationId(), null, null, null, null, null, null, null, null, null);
    }

    private CommandExecutionResult auditAndReturn(
            CommandExecuteRequest request,
            String message,
            String projectId,
            String conversationId,
            String planId,
            String assetId,
            List<String> memoryIds,
            List<Project> projects,
            List<Conversation> conversations,
            List<Memory> memories,
            List<Asset> assets,
            List<Skill> skills,
            List<Node> nodes
    ) {
        auditService.record("system", "command", "command", request.commandId().value(), "execute", request.args() == null ? "{}" : request.args().toString(), message);
        return new CommandExecutionResult(
                request.commandId().value(),
                message,
                projectId,
                conversationId,
                planId,
                assetId,
                memoryIds,
                projects,
                conversations,
                memories,
                assets,
                nodes,
                skills,
                projectService.recentAuditLogs()
        );
    }

    private String requireArg(CommandExecuteRequest request, String key) {
        if (request.args() == null || !request.args().containsKey(key) || request.args().get(key).isBlank()) {
            throw new BadRequestException("缺少必填参数: " + key);
        }
        return request.args().get(key);
    }

    private List<String> tags(CommandExecuteRequest request) {
        var args = safeArgs(request);
        if (args.get("tags") == null || args.get("tags").isBlank()) {
            return List.of();
        }
        return List.of(args.get("tags").split(","));
    }

    private java.util.Map<String, String> safeArgs(CommandExecuteRequest request) {
        return request.args() == null ? java.util.Map.of() : request.args();
    }
}
