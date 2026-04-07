package com.loom.server.controller;

import com.loom.server.api.ApiEnvelope;
import com.loom.server.service.BootstrapService;
import com.loom.server.support.Responses.BootstrapResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bootstrap")
public class BootstrapController {

    private final BootstrapService bootstrapService;

    public BootstrapController(BootstrapService bootstrapService) {
        this.bootstrapService = bootstrapService;
    }

    @GetMapping
    public ApiEnvelope<BootstrapResponse> get() {
        return ApiEnvelope.of(bootstrapService.load());
    }
}
