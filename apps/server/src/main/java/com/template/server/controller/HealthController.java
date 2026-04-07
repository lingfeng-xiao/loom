package com.template.server.controller;

import com.template.server.api.ApiEnvelope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public ApiEnvelope<Map<String, Object>> health() {
        return ApiEnvelope.of(Map.of(
                "status", "ok",
                "service", "template-server",
                "time", Instant.now().toString()
        ));
    }
}
