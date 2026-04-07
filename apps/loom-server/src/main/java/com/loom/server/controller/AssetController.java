package com.loom.server.controller;

import com.loom.server.api.ApiEnvelope;
import com.loom.server.dto.AssetFromConversationRequest;
import com.loom.server.dto.AssetFromPlanRequest;
import com.loom.server.model.Asset;
import com.loom.server.service.AssetService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @GetMapping
    public ApiEnvelope<List<Asset>> list() {
        return ApiEnvelope.of(assetService.listAssets());
    }

    @GetMapping("/{id}")
    public ApiEnvelope<Asset> get(@PathVariable String id) {
        return ApiEnvelope.of(assetService.getAsset(id));
    }

    @PostMapping("/from-conversation")
    public ApiEnvelope<Asset> createFromConversation(@Valid @RequestBody AssetFromConversationRequest request) {
        return ApiEnvelope.of(assetService.createFromConversation(request));
    }

    @PostMapping("/from-plan")
    public ApiEnvelope<Asset> createFromPlan(@Valid @RequestBody AssetFromPlanRequest request) {
        return ApiEnvelope.of(assetService.createFromPlan(request));
    }
}
