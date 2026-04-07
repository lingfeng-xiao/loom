package com.loom.server.service;

import com.loom.server.config.LoomProperties;
import com.loom.server.dto.AppearanceSettingsUpdateRequest;
import com.loom.server.dto.ModelSettingsUpdateRequest;
import com.loom.server.dto.VaultSettingsUpdateRequest;
import com.loom.server.dto.WorkspaceNodeSettingsUpdateRequest;
import com.loom.server.dto.WorkspaceSettingsUpdateRequest;
import com.loom.server.model.AppearanceSettings;
import com.loom.server.model.CommandId;
import com.loom.server.model.ModelSettings;
import com.loom.server.model.VaultSettings;
import com.loom.server.model.WorkspaceNodeSettings;
import com.loom.server.model.WorkspaceSettings;
import com.loom.server.repository.ProjectRepository;
import com.loom.server.repository.WorkspaceSettingsRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceSettingsService {

    private final WorkspaceSettingsRepository workspaceSettingsRepository;
    private final ProjectRepository projectRepository;
    private final LoomProperties properties;
    private final SkillService skillService;

    public WorkspaceSettingsService(
            WorkspaceSettingsRepository workspaceSettingsRepository,
            ProjectRepository projectRepository,
            LoomProperties properties,
            SkillService skillService
    ) {
        this.workspaceSettingsRepository = workspaceSettingsRepository;
        this.projectRepository = projectRepository;
        this.properties = properties;
        this.skillService = skillService;
    }

    public WorkspaceSettings getSettings() {
        return workspaceSettingsRepository.find().orElseGet(() -> {
            String defaultProjectId = projectRepository.findAll().stream().findFirst().map(project -> project.id()).orElse(null);
            WorkspaceSettings seeded = new WorkspaceSettings(
                    "Loom",
                    "zh-CN",
                    "comfortable",
                    defaultProjectId,
                    "last_conversation",
                    true,
                    new ModelSettings(
                            properties.getAi().getProviderLabel(),
                            properties.getAi().getBaseUrl(),
                            properties.getAi().getModel(),
                            properties.getAi().getTemperature()
                    ),
                    new VaultSettings(
                            properties.getStorage().getServerVaultRoot(),
                            properties.getStorage().getLocalVaultRoot(),
                            "/{vault}/{project-slug}/{asset-type}/{yyyy}/{mm}/{slug}.md",
                            "server"
                    ),
                    new WorkspaceNodeSettings(
                            properties.getNodes().getHeartbeatTimeoutSeconds(),
                            true,
                            "JD 中心节点"
                    ),
                    new AppearanceSettings(
                            "light",
                            "comfortable",
                            "blue-teal",
                            "Geist Sans",
                            "JetBrains Mono"
                    ),
                    List.of(CommandId.values()),
                    skillService.listSkills().stream().map(skill -> skill.id()).toList(),
                    Instant.now()
            );
            workspaceSettingsRepository.save(seeded);
            return seeded;
        });
    }

    public WorkspaceSettings updateSettings(WorkspaceSettingsUpdateRequest request) {
        WorkspaceSettings current = getSettings();
        WorkspaceSettings updated = new WorkspaceSettings(
                valueOrDefault(request.workspaceName(), current.workspaceName()),
                valueOrDefault(request.language(), current.language()),
                valueOrDefault(request.density(), current.density()),
                request.defaultProjectId() == null ? current.defaultProjectId() : request.defaultProjectId(),
                valueOrDefault(request.defaultLandingView(), current.defaultLandingView()),
                request.inspectorDefaultOpen() == null ? current.inspectorDefaultOpen() : request.inspectorDefaultOpen(),
                mergeModel(current.model(), request.model()),
                mergeVault(current.vault(), request.vault()),
                mergeNodes(current.nodes(), request.nodes()),
                mergeAppearance(current.appearance(), request.appearance()),
                request.enabledCommands() == null ? current.enabledCommands() : List.copyOf(request.enabledCommands()),
                request.enabledSkills() == null ? current.enabledSkills() : List.copyOf(request.enabledSkills()),
                Instant.now()
        );
        workspaceSettingsRepository.save(updated);
        return updated;
    }

    private ModelSettings mergeModel(ModelSettings current, ModelSettingsUpdateRequest request) {
        if (request == null) {
            return current;
        }
        return new ModelSettings(
                valueOrDefault(request.providerLabel(), current.providerLabel()),
                valueOrDefault(request.baseUrl(), current.baseUrl()),
                valueOrDefault(request.model(), current.model()),
                request.temperature() == null ? current.temperature() : request.temperature()
        );
    }

    private VaultSettings mergeVault(VaultSettings current, VaultSettingsUpdateRequest request) {
        if (request == null) {
            return current;
        }
        return new VaultSettings(
                valueOrDefault(request.serverVaultRoot(), current.serverVaultRoot()),
                valueOrDefault(request.localVaultRoot(), current.localVaultRoot()),
                valueOrDefault(request.assetPathTemplate(), current.assetPathTemplate()),
                valueOrDefault(request.writeTarget(), current.writeTarget())
        );
    }

    private WorkspaceNodeSettings mergeNodes(WorkspaceNodeSettings current, WorkspaceNodeSettingsUpdateRequest request) {
        if (request == null) {
            return current;
        }
        return new WorkspaceNodeSettings(
                request.heartbeatTimeoutSeconds() == null ? current.heartbeatTimeoutSeconds() : request.heartbeatTimeoutSeconds(),
                request.inspectorShowOffline() == null ? current.inspectorShowOffline() : request.inspectorShowOffline(),
                valueOrDefault(request.centerNodeLabel(), current.centerNodeLabel())
        );
    }

    private AppearanceSettings mergeAppearance(AppearanceSettings current, AppearanceSettingsUpdateRequest request) {
        if (request == null) {
            return current;
        }
        return new AppearanceSettings(
                valueOrDefault(request.theme(), current.theme()),
                valueOrDefault(request.density(), current.density()),
                valueOrDefault(request.accentTone(), current.accentTone()),
                valueOrDefault(request.fontSans(), current.fontSans()),
                valueOrDefault(request.fontMono(), current.fontMono())
        );
    }

    private String valueOrDefault(String next, String current) {
        return next == null || next.isBlank() ? current : next;
    }
}
