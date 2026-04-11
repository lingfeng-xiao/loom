package com.loom.server.controller;

import com.loom.server.api.ApiEnvelope;
import com.loom.server.model.BootstrapPayload;
import com.loom.server.service.LoomBootstrapService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BootstrapController {

    private final LoomBootstrapService bootstrapService;

    public BootstrapController(LoomBootstrapService bootstrapService) {
        this.bootstrapService = bootstrapService;
    }

    @GetMapping("/api/bootstrap")
    public ApiEnvelope<BootstrapPayload> bootstrap() {
        return ApiEnvelope.of(bootstrapService.getBootstrap());
    }
}
