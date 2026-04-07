package com.loom.server.controller;

import com.loom.server.api.ApiEnvelope;
import com.loom.server.dto.WorkspaceSettingsUpdateRequest;
import com.loom.server.model.WorkspaceSettings;
import com.loom.server.service.WorkspaceSettingsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings/workspace")
public class WorkspaceSettingsController {

    private final WorkspaceSettingsService workspaceSettingsService;

    public WorkspaceSettingsController(WorkspaceSettingsService workspaceSettingsService) {
        this.workspaceSettingsService = workspaceSettingsService;
    }

    @GetMapping
    public ApiEnvelope<WorkspaceSettings> get() {
        return ApiEnvelope.of(workspaceSettingsService.getSettings());
    }

    @PutMapping
    public ApiEnvelope<WorkspaceSettings> update(@RequestBody WorkspaceSettingsUpdateRequest request) {
        return ApiEnvelope.of(workspaceSettingsService.updateSettings(request));
    }
}
