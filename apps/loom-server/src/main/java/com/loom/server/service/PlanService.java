package com.loom.server.service;

import com.loom.server.dto.PlanCreateRequest;
import com.loom.server.dto.PlanStepCreateRequest;
import com.loom.server.error.BadRequestException;
import com.loom.server.model.Conversation;
import com.loom.server.model.Plan;
import com.loom.server.model.PlanExecutionResult;
import com.loom.server.model.PlanStatus;
import com.loom.server.model.PlanStep;
import com.loom.server.model.PlanStepStatus;
import com.loom.server.repository.PlanRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PlanService {

    private final PlanRepository planRepository;
    private final ProjectService projectService;
    private final ConversationService conversationService;
    private final AuditService auditService;

    public PlanService(
            PlanRepository planRepository,
            ProjectService projectService,
            ConversationService conversationService,
            AuditService auditService
    ) {
        this.planRepository = planRepository;
        this.projectService = projectService;
        this.conversationService = conversationService;
        this.auditService = auditService;
    }

    public List<Plan> listPlans() {
        return planRepository.findAll();
    }

    public Plan getPlan(String id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("未找到计划 " + id));
    }

    public Plan createPlan(PlanCreateRequest request) {
        List<PlanStep> steps = new ArrayList<>();
        if (request.steps() != null) {
            for (int index = 0; index < request.steps().size(); index++) {
                PlanStepCreateRequest step = request.steps().get(index);
                steps.add(new PlanStep(
                        UUID.randomUUID().toString(),
                        step.title() == null ? "步骤 " + (index + 1) : step.title(),
                        step.description() == null ? "" : step.description(),
                        PlanStepStatus.PENDING,
                        "",
                        step.sortOrder() == null ? index + 1 : step.sortOrder()
                ));
            }
        }
        return createPlan(request.projectId(), request.conversationId(), request.goal(), request.constraints(), request.approvalRequired(), steps);
    }

    public Plan createPlan(String projectId, String conversationId, String goal, Map<String, String> args) {
        List<String> constraints = args == null || args.get("constraints") == null ? List.of() : List.of(args.get("constraints").split("\\|"));
        boolean approvalRequired = args == null || !Boolean.FALSE.toString().equalsIgnoreCase(args.get("approvalRequired"));
        List<PlanStep> steps = new ArrayList<>();
        if (args != null && args.get("steps") != null) {
            String[] values = args.get("steps").split("\\|");
            for (int index = 0; index < values.length; index++) {
                steps.add(new PlanStep(UUID.randomUUID().toString(), values[index], values[index], PlanStepStatus.PENDING, "", index + 1));
            }
        }
        return createPlan(projectId, conversationId, goal, constraints, approvalRequired, steps);
    }

    private Plan createPlan(String projectId, String conversationId, String goal, List<String> constraints, boolean approvalRequired, List<PlanStep> steps) {
        projectService.getProject(projectId);
        Conversation conversation = conversationService.getConversation(conversationId);
        if (!conversation.projectId().equals(projectId)) {
            throw new BadRequestException("会话不属于当前项目");
        }

        Instant now = Instant.now();
        Plan plan = new Plan(
                UUID.randomUUID().toString(),
                projectId,
                conversationId,
                goal,
                constraints == null ? List.of() : List.copyOf(constraints),
                PlanStatus.DRAFT,
                approvalRequired,
                steps == null || steps.isEmpty() ? defaultSteps(goal) : List.copyOf(steps),
                null,
                now,
                now
        );
        planRepository.save(plan);
        auditService.record("system", "plan-service", "plan", plan.id(), "create", goal, "ok");
        return plan;
    }

    public Plan approvePlan(String id, String approvedBy) {
        Plan plan = getPlan(id);
        if (plan.status() != PlanStatus.DRAFT && plan.status() != PlanStatus.READY) {
            throw new BadRequestException("只有 draft 或 ready 状态的计划可以批准");
        }
        Plan approved = new Plan(
                plan.id(),
                plan.projectId(),
                plan.conversationId(),
                plan.goal(),
                plan.constraints(),
                PlanStatus.APPROVED,
                plan.approvalRequired(),
                plan.steps(),
                plan.executionResult(),
                plan.createdAt(),
                Instant.now()
        );
        planRepository.save(approved);
        auditService.record("system", "plan-service", "plan", id, "approve", approvedBy, "ok");
        return approved;
    }

    public Plan runPlan(String id) {
        Plan plan = getPlan(id);
        if (plan.approvalRequired() && plan.status() != PlanStatus.APPROVED) {
            throw new BadRequestException("计划执行前必须先批准");
        }
        if (plan.status() == PlanStatus.COMPLETED) {
            throw new BadRequestException("已完成计划不能再次执行");
        }
        Plan running = new Plan(
                plan.id(),
                plan.projectId(),
                plan.conversationId(),
                plan.goal(),
                plan.constraints(),
                PlanStatus.RUNNING,
                plan.approvalRequired(),
                markSteps(plan.steps(), PlanStepStatus.RUNNING),
                plan.executionResult(),
                plan.createdAt(),
                Instant.now()
        );
        planRepository.save(running);
        auditService.record("system", "plan-service", "plan", id, "run", plan.goal(), "ok");
        return running;
    }

    public Plan completePlan(String id, String summary, List<String> outputAssetIds, List<String> logs) {
        Plan plan = getPlan(id);
        if (plan.status() != PlanStatus.RUNNING && plan.status() != PlanStatus.APPROVED) {
            throw new BadRequestException("计划必须处于 running 或 approved 状态后才能完成");
        }
        PlanExecutionResult result = new PlanExecutionResult(
                summary == null ? "计划已完成" : summary,
                outputAssetIds == null ? List.of() : List.copyOf(outputAssetIds),
                logs == null ? List.of() : List.copyOf(logs)
        );
        Plan completed = new Plan(
                plan.id(),
                plan.projectId(),
                plan.conversationId(),
                plan.goal(),
                plan.constraints(),
                PlanStatus.COMPLETED,
                plan.approvalRequired(),
                markSteps(plan.steps(), PlanStepStatus.COMPLETED),
                result,
                plan.createdAt(),
                Instant.now()
        );
        planRepository.save(completed);
        auditService.record("system", "plan-service", "plan", id, "complete", result.summary(), "ok");
        return completed;
    }

    private List<PlanStep> defaultSteps(String goal) {
        return List.of(
                new PlanStep(UUID.randomUUID().toString(), "理解目标", goal, PlanStepStatus.PENDING, "", 1),
                new PlanStep(UUID.randomUUID().toString(), "收集上下文", "整理项目和会话上下文", PlanStepStatus.PENDING, "", 2),
                new PlanStep(UUID.randomUUID().toString(), "执行并记录", "执行计划并沉淀结果", PlanStepStatus.PENDING, "", 3)
        );
    }

    private List<PlanStep> markSteps(List<PlanStep> steps, PlanStepStatus status) {
        List<PlanStep> updated = new ArrayList<>();
        for (PlanStep step : steps) {
            updated.add(new PlanStep(step.id(), step.title(), step.description(), status, step.result(), step.sortOrder()));
        }
        return updated;
    }
}
