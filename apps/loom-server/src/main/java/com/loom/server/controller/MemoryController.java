package com.loom.server.controller;

import com.loom.server.api.ApiEnvelope;
import com.loom.server.dto.MemoryCreateRequest;
import com.loom.server.dto.MemoryUpdateRequest;
import com.loom.server.model.Memory;
import com.loom.server.service.MemoryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/memories")
public class MemoryController {

    private final MemoryService memoryService;

    public MemoryController(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @GetMapping
    public ApiEnvelope<List<Memory>> list(
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) String scope
    ) {
        return ApiEnvelope.of(memoryService.listMemories(projectId, scope));
    }

    @PostMapping
    public ApiEnvelope<Memory> create(@Valid @RequestBody MemoryCreateRequest request) {
        return ApiEnvelope.of(memoryService.createMemory(request));
    }

    @PutMapping("/{memoryId}")
    public ApiEnvelope<Memory> update(@PathVariable String memoryId, @RequestBody MemoryUpdateRequest request) {
        return ApiEnvelope.of(memoryService.updateMemory(memoryId, request));
    }

    @PostMapping("/{memoryId}/promote")
    public ApiEnvelope<Memory> promote(@PathVariable String memoryId) {
        return ApiEnvelope.of(memoryService.promoteMemory(memoryId));
    }
}
