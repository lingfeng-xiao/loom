package com.loom.server.workspace;

import com.loom.server.api.ApiException;
import com.loom.server.workspace.WorkspaceDtos.LlmConfigView;
import com.loom.server.workspace.WorkspaceDtos.LlmProviderPresetView;
import com.loom.server.workspace.WorkspaceDtos.UpdateLlmConfigRequest;
import org.springframework.http.HttpStatus;

import java.util.List;

final class LlmConfigState {
    String id;
    String presetId;
    String provider;
    String displayName;
    String apiBaseUrl;
    String modelId;
    String apiKey;
    String systemPrompt;
    double temperature;
    Integer maxTokens;
    int timeoutMs;
    boolean active;
    String updatedAt;

    LlmConfigState(
            String id,
            String presetId,
            String provider,
            String displayName,
            String apiBaseUrl,
            String modelId,
            String apiKey,
            String systemPrompt,
            double temperature,
            Integer maxTokens,
            int timeoutMs,
            boolean active,
            String updatedAt
    ) {
        this.id = id;
        this.presetId = presetId;
        this.provider = provider;
        this.displayName = displayName;
        this.apiBaseUrl = apiBaseUrl;
        this.modelId = modelId;
        this.apiKey = apiKey;
        this.systemPrompt = systemPrompt;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.timeoutMs = timeoutMs;
        this.active = active;
        this.updatedAt = updatedAt;
    }

    boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    void apply(UpdateLlmConfigRequest request, List<LlmProviderPresetView> presets) {
        if (request == null) {
            return;
        }

        if (request.presetId() != null && !request.presetId().isBlank()) {
            LlmProviderPresetView preset = presets.stream()
                    .filter(candidate -> candidate.id().equals(request.presetId()))
                    .findFirst()
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "LLM_PRESET_NOT_FOUND", "Unknown LLM preset"));
            presetId = preset.id();
            provider = preset.provider();
            displayName = preset.label();
            apiBaseUrl = preset.apiBaseUrl();
            modelId = preset.defaultModelId();
        }

        id = WorkspaceStateService.blankTo(request.profileId(), id);
        displayName = WorkspaceStateService.blankTo(request.displayName(), displayName);
        apiBaseUrl = WorkspaceStateService.blankTo(request.apiBaseUrl(), apiBaseUrl);
        modelId = WorkspaceStateService.blankTo(request.modelId(), modelId);
        systemPrompt = WorkspaceStateService.blankTo(request.systemPrompt(), systemPrompt);
        apiKey = request.apiKey() == null ? apiKey : request.apiKey().trim();
        temperature = request.temperature() == null ? temperature : request.temperature();
        maxTokens = request.maxTokens() == null ? maxTokens : request.maxTokens();
        timeoutMs = request.timeoutMs() == null ? timeoutMs : request.timeoutMs();
        active = request.activate() == null ? active : request.activate();
    }

    LlmConfigState copy() {
        return new LlmConfigState(id, presetId, provider, displayName, apiBaseUrl, modelId, apiKey, systemPrompt, temperature, maxTokens, timeoutMs, active, updatedAt);
    }

    LlmConfigView toView() {
        return new LlmConfigView(
                id,
                presetId,
                provider,
                displayName,
                apiBaseUrl,
                modelId,
                isConfigured(),
                active,
                WorkspaceStateService.maskApiKey(apiKey),
                systemPrompt,
                temperature,
                maxTokens,
                timeoutMs,
                updatedAt
        );
    }
}
