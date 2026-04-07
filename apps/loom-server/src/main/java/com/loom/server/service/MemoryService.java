package com.loom.server.service;

import com.loom.server.dto.MemoryCreateRequest;
import com.loom.server.dto.MemoryUpdateRequest;
import com.loom.server.error.NotFoundException;
import com.loom.server.model.Memory;
import com.loom.server.model.MemoryScope;
import com.loom.server.model.MemoryStatus;
import com.loom.server.repository.MemoryRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MemoryService {

    private final MemoryRepository memoryRepository;
    private final ProjectService projectService;
    private final AuditService auditService;

    public MemoryService(MemoryRepository memoryRepository, ProjectService projectService, AuditService auditService) {
        this.memoryRepository = memoryRepository;
        this.projectService = projectService;
        this.auditService = auditService;
    }

    public List<Memory> listMemories(String projectId, String scopeValue) {
        return memoryRepository.findVisible(projectId, scopeValue);
    }

    public Memory createMemory(MemoryCreateRequest request) {
        if (request.scope() == MemoryScope.PROJECT && (request.projectId() == null || request.projectId().isBlank())) {
            throw new IllegalArgumentException("项目记忆必须提供 projectId");
        }
        if (request.projectId() != null && !request.projectId().isBlank()) {
            projectService.getProject(request.projectId());
        }
        Instant now = Instant.now();
        Memory memory = new Memory(
                UUID.randomUUID().toString(),
                request.scope(),
                request.projectId(),
                request.title(),
                request.content(),
                request.priority() == null ? 0 : request.priority(),
                MemoryStatus.ACTIVE,
                request.sourceType() == null ? "manual" : request.sourceType(),
                request.sourceRef(),
                now,
                now
        );
        memoryRepository.save(memory);
        if (memory.projectId() != null) {
            projectService.linkMemory(memory.projectId(), memory.id());
        }
        auditService.record("system", "memory-service", "memory", memory.id(), "create", memory.content(), "ok");
        return memory;
    }

    public Memory createMemoryFromCommand(String projectId, String scope, String title, String content, java.util.Map<String, String> args) {
        MemoryScope memoryScope = "global".equalsIgnoreCase(scope) ? MemoryScope.GLOBAL : MemoryScope.PROJECT;
        MemoryCreateRequest request = new MemoryCreateRequest(
                memoryScope,
                memoryScope == MemoryScope.GLOBAL ? null : projectId,
                title,
                content,
                args == null ? null : parseInt(args.get("priority")),
                args == null ? null : args.get("sourceType"),
                args == null ? null : args.get("sourceRef")
        );
        return createMemory(request);
    }

    public Memory updateMemory(String id, MemoryUpdateRequest request) {
        Memory current = getMemory(id);
        Memory updated = new Memory(
                current.id(),
                request.scope() == null ? current.scope() : request.scope(),
                request.projectId() == null ? current.projectId() : request.projectId(),
                request.title() == null ? current.title() : request.title(),
                request.content() == null ? current.content() : request.content(),
                request.priority() == null ? current.priority() : request.priority(),
                request.status() == null ? current.status() : request.status(),
                request.sourceType() == null ? current.sourceType() : request.sourceType(),
                request.sourceRef() == null ? current.sourceRef() : request.sourceRef(),
                current.createdAt(),
                Instant.now()
        );
        memoryRepository.save(updated);
        auditService.record("system", "memory-service", "memory", id, "update", updated.content(), "ok");
        return updated;
    }

    public Memory promoteMemory(String id) {
        Memory current = getMemory(id);
        Memory promoted = new Memory(
                current.id(),
                MemoryScope.DERIVED,
                current.projectId(),
                current.title(),
                current.content(),
                current.priority() + 1,
                MemoryStatus.ACTIVE,
                current.sourceType(),
                current.sourceRef(),
                current.createdAt(),
                Instant.now()
        );
        memoryRepository.save(promoted);
        auditService.record("system", "memory-service", "memory", id, "promote", promoted.content(), "ok");
        return promoted;
    }

    public Memory getMemory(String id) {
        return memoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("未找到记忆 " + id));
    }

    private Integer parseInt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("整数参数不合法 " + value, exception);
        }
    }
}
