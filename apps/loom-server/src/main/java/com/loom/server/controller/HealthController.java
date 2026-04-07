package com.loom.server.controller;

import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @Value("${spring.application.name:loom-server}")
    private String appName;

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "ok",
                "app", appName,
                "java", System.getProperty("java.version"),
                "timestamp", Instant.now().toString()
        );
    }
}
