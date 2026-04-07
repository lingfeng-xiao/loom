package com.loom.server.service;

import com.loom.server.dto.AssetFromConversationRequest;
import com.loom.server.dto.AssetFromPlanRequest;
import com.loom.server.error.NotFoundException;
import com.loom.server.model.Asset;
import com.loom.server.model.AssetType;
import com.loom.server.model.Conversation;
import com.loom.server.model.Plan;
import com.loom.server.model.Project;
import com.loom.server.repository.AssetRepository;
import com.loom.server.store.ObsidianAssetWriter;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AssetService {

    private final AssetRepository assetRepository;
    private final ProjectService projectService;
    private final ConversationService conversationService;
    private final PlanService planService;
    private final ObsidianAssetWriter writer;
    private final LoomStoragePaths storagePaths;
    private final AuditService auditService;

    public AssetService(
            AssetRepository assetRepository,
            ProjectService projectService,
            ConversationService conversationService,
            PlanService planService,
            ObsidianAssetWriter writer,
            LoomStoragePaths storagePaths,
            AuditService auditService
    ) {
        this.assetRepository = assetRepository;
        this.projectService = projectService;
        this.conversationService = conversationService;
        this.planService = planService;
        this.writer = writer;
        this.storagePaths = storagePaths;
        this.auditService = auditService;
    }

    public List<Asset> listAssets() {
        return assetRepository.findAll();
    }

    public List<Asset> listAssetsByProject(String projectId) {
        return assetRepository.findByProject(projectId);
    }

    public Asset getAsset(String id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("未找到资产 " + id));
    }

    public Asset createFromConversation(AssetFromConversationRequest request) {
        Conversation conversation = conversationService.getConversation(request.conversationId());
        Project project = projectService.getProject(conversation.projectId());
        Path storagePath = writer.writeAsset(
                storagePaths.serverVaultRoot(),
                project,
                AssetType.KNOWLEDGE_CARD,
                request.title(),
                request.content(),
                request.tags()
        );
        Asset asset = toAsset(project, AssetType.KNOWLEDGE_CARD, request.title(), request.content(), storagePath, conversation.id(), null, null, request.tags());
        assetRepository.save(asset);
        auditService.record("system", "asset-service", "asset", asset.id(), "create_from_conversation", request.content(), "ok");
        return asset;
    }

    public Asset createFromPlan(AssetFromPlanRequest request) {
        Plan plan = planService.getPlan(request.planId());
        Project project = projectService.getProject(plan.projectId());
        Path storagePath = writer.writeAsset(
                storagePaths.serverVaultRoot(),
                project,
                AssetType.SUMMARY_NOTE,
                request.title(),
                request.content(),
                request.tags()
        );
        Asset asset = toAsset(project, AssetType.SUMMARY_NOTE, request.title(), request.content(), storagePath, plan.conversationId(), plan.id(), null, request.tags());
        assetRepository.save(asset);
        auditService.record("system", "asset-service", "asset", asset.id(), "create_from_plan", request.content(), "ok");
        return asset;
    }

    public Asset createManualAsset(String projectId, AssetType type, String title, String content, List<String> tags) {
        Project project = projectService.getProject(projectId);
        Path storagePath = writer.writeAsset(storagePaths.serverVaultRoot(), project, type, title, content, tags);
        Asset asset = toAsset(project, type, title, content, storagePath, null, null, null, tags);
        assetRepository.save(asset);
        auditService.record("system", "asset-service", "asset", asset.id(), "create_manual", title, "ok");
        return asset;
    }

    private Asset toAsset(Project project, AssetType type, String title, String contentRef, Path storagePath, String conversationId, String planId, String nodeId, List<String> tags) {
        return new Asset(
                UUID.randomUUID().toString(),
                project.id(),
                type,
                title,
                contentRef,
                storagePath.toString(),
                conversationId,
                planId,
                nodeId,
                tags == null ? List.of() : List.copyOf(tags),
                Instant.now()
        );
    }
}
