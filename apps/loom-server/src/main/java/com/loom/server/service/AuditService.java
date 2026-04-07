package com.loom.server.service;

import com.loom.server.model.AuditLogEntry;
import com.loom.server.repository.AuditLogRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public AuditLogEntry record(String actor, String source, String targetType, String targetId, String action, String payload, String result) {
        AuditLogEntry entry = new AuditLogEntry(
                UUID.randomUUID().toString(),
                actor,
                source,
                targetType,
                targetId,
                action,
                digest(payload == null ? "" : payload),
                result,
                Instant.now()
        );
        auditLogRepository.save(entry);
        return entry;
    }

    public List<AuditLogEntry> recent(int limit) {
        return auditLogRepository.recent(limit);
    }

    private String digest(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
