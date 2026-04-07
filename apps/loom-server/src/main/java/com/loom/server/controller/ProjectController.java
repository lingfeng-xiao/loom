package com.loom.server.controller;

import com.loom.server.api.ApiEnvelope;
import com.loom.server.dto.ProjectCreateRequest;
import com.loom.server.dto.ProjectSwitchRequest;
import com.loom.server.dto.ProjectUpdateRequest;
import com.loom.server.model.Project;
import com.loom.server.service.ProjectService;
import com.loom.server.support.Responses.DashboardResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public ApiEnvelope<List<Project>> list() {
        return ApiEnvelope.of(projectService.listProjects());
    }

    @PostMapping
    public ApiEnvelope<Project> create(@Valid @RequestBody ProjectCreateRequest request) {
        return ApiEnvelope.of(projectService.createProject(request));
    }

    @GetMapping("/{projectId}")
    public ApiEnvelope<Project> get(@PathVariable String projectId) {
        return ApiEnvelope.of(projectService.getProject(projectId));
    }

    @PutMapping("/{projectId}")
    public ApiEnvelope<Project> update(@PathVariable String projectId, @RequestBody ProjectUpdateRequest request) {
        return ApiEnvelope.of(projectService.updateProject(projectId, request));
    }

    @PostMapping("/{projectId}/switch")
    public ApiEnvelope<Project> switchProject(@PathVariable String projectId, @RequestBody(required = false) ProjectSwitchRequest request) {
        String targetProjectId = request == null ? projectId : request.projectId();
        return ApiEnvelope.of(projectService.switchProject(targetProjectId));
    }

    @GetMapping("/{projectId}/dashboard")
    public ApiEnvelope<DashboardResponse> dashboard(@PathVariable String projectId) {
        return ApiEnvelope.of(projectService.dashboard(projectId));
    }
}
