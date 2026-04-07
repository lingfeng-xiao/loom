package com.template.server.controller;

import com.template.server.api.ApiEnvelope;
import com.template.server.model.BootstrapPayload;
import com.template.server.service.TemplateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BootstrapController {

    private final TemplateService templateService;

    public BootstrapController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping("/api/bootstrap")
    public ApiEnvelope<BootstrapPayload> bootstrap() {
        return ApiEnvelope.of(templateService.getBootstrap());
    }
}
