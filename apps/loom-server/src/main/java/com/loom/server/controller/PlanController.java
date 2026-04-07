package com.loom.server.controller;

import com.loom.server.api.ApiEnvelope;
import com.loom.server.dto.PlanApproveRequest;
import com.loom.server.dto.PlanCompleteRequest;
import com.loom.server.dto.PlanCreateRequest;
import com.loom.server.model.Plan;
import com.loom.server.service.PlanService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plans")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    public ApiEnvelope<List<Plan>> list() {
        return ApiEnvelope.of(planService.listPlans());
    }

    @GetMapping("/{planId}")
    public ApiEnvelope<Plan> get(@PathVariable String planId) {
        return ApiEnvelope.of(planService.getPlan(planId));
    }

    @PostMapping
    public ApiEnvelope<Plan> create(@Valid @RequestBody PlanCreateRequest request) {
        return ApiEnvelope.of(planService.createPlan(request));
    }

    @PostMapping("/{planId}/approve")
    public ApiEnvelope<Plan> approve(@PathVariable String planId, @RequestBody(required = false) PlanApproveRequest request) {
        return ApiEnvelope.of(planService.approvePlan(planId, request == null ? "system" : request.approvedBy()));
    }

    @PostMapping("/{planId}/run")
    public ApiEnvelope<Plan> run(@PathVariable String planId) {
        return ApiEnvelope.of(planService.runPlan(planId));
    }

    @PostMapping("/{planId}/complete")
    public ApiEnvelope<Plan> complete(@PathVariable String planId, @RequestBody(required = false) PlanCompleteRequest request) {
        PlanCompleteRequest safe = request == null ? new PlanCompleteRequest(null, List.of(), List.of()) : request;
        return ApiEnvelope.of(planService.completePlan(planId, safe.summary(), safe.outputAssetIds(), safe.logs()));
    }
}
