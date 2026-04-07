package com.template.server.controller;

import com.template.server.api.ApiEnvelope;
import com.template.server.dto.WorkspaceSettingsUpdateRequest;
import com.template.server.model.WorkspaceSettings;
import com.template.server.repository.SettingsRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WorkspaceSettingsController {

    private final SettingsRepository settingsRepository;

    public WorkspaceSettingsController(SettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    @GetMapping("/api/settings")
    public ApiEnvelope<WorkspaceSettings> getSettings() {
        return ApiEnvelope.of(settingsRepository.get());
    }

    @PutMapping("/api/settings")
    public ApiEnvelope<WorkspaceSettings> updateSettings(@Valid @RequestBody WorkspaceSettingsUpdateRequest request) {
        return ApiEnvelope.of(settingsRepository.update(request));
    }
}
