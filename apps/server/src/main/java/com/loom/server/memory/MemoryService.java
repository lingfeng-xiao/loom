package com.loom.server.memory;

import com.loom.server.api.ApiException;
import com.loom.server.workspace.WorkspaceDtos.CursorPage;
import com.loom.server.workspace.WorkspaceDtos.MemoryItemView;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class MemoryService {
    private static final int DEFAULT_LIMIT = 50;

    private final MemoryItemRepository memoryItemRepository;
    private final MemorySuggestionRepository memorySuggestionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public MemoryService(
            MemoryItemRepository memoryItemRepository,
            MemorySuggestionRepository memorySuggestionRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.memoryItemRepository = memoryItemRepository;
        this.memorySuggestionRepository = memorySuggestionRepository;
        this.eventPublisher = eventPublisher;
    }

    public CursorPage<MemoryItemView> listMemory(MemoryQuery query) {
        String projectId = requireText(query.projectId(), "projectId");
        List<MemoryItemView> allItems = memoryItemRepository.listByProject(projectId).stream()
                .map(MemoryItemRecord::toView)
                .filter(item -> matchesScope(query.scope(), query.conversationId(), projectId, item))
                .sorted(Comparator.comparing(MemoryItemView::updatedAt).reversed())
                .toList();
        return page(allItems, query.cursor(), query.limit());
    }

    public MemoryItemView createMemoryItem(MemoryItemCommand command) {
        MemoryScope scope = resolveScope(command.scope(), command.conversationId());
        MemoryItemRecord record = memoryItemRepository.save(new MemoryItemRecord(
                "memory-item-" + UUID.randomUUID(),
                scope.wireValue(),
                resolveProjectId(command.projectId(), scope),
                resolveConversationId(scope, command.conversationId()),
                requireText(command.content(), "content"),
                MemorySource.from(command.source()).wireValue(),
                now()
        ));
        return record.toView();
    }

    public MemoryItemView updateMemoryItem(String projectId, String memoryId, MemoryItemCommand command) {
        requireText(projectId, "projectId");
        MemoryItemRecord existing = memoryItemRepository.findById(requireText(memoryId, "memoryId"))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MEMORY_NOT_FOUND", "Memory item does not exist"));
        if (!projectId.equals(existing.projectId()) && existing.projectId() != null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "MEMORY_NOT_FOUND", "Memory item does not exist in this project");
        }
        MemoryScope scope = resolveScope(command.scope(), command.conversationId());
        MemoryItemRecord updated = memoryItemRepository.save(new MemoryItemRecord(
                existing.id(),
                scope.wireValue(),
                resolveProjectId(command.projectId(), scope, existing.projectId()),
                resolveConversationId(scope, command.conversationId()),
                requireText(command.content(), "content"),
                MemorySource.from(command.source()).wireValue(),
                now()
        ));
        return updated.toView();
    }

    public void deleteMemoryItem(String projectId, String memoryId) {
        requireText(projectId, "projectId");
        MemoryItemRecord existing = memoryItemRepository.findById(requireText(memoryId, "memoryId"))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MEMORY_NOT_FOUND", "Memory item does not exist"));
        if (existing.projectId() != null && !projectId.equals(existing.projectId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "MEMORY_NOT_FOUND", "Memory item does not exist in this project");
        }
        memoryItemRepository.deleteById(existing.id());
    }

    public CursorPage<MemorySuggestionView> listSuggestions(MemoryQuery query) {
        String projectId = requireText(query.projectId(), "projectId");
        List<MemorySuggestionView> allSuggestions = memorySuggestionRepository.listByProject(projectId).stream()
                .map(MemorySuggestionRecord::toView)
                .filter(suggestion -> matchesSuggestionScope(query.scope(), query.conversationId(), projectId, suggestion))
                .sorted(Comparator.comparing(MemorySuggestionView::createdAt).reversed())
                .toList();
        return page(allSuggestions, query.cursor(), query.limit());
    }

    public MemorySuggestionView suggestMemory(MemorySuggestionCommand command) {
        MemoryScope scope = resolveScope(command.scope(), command.conversationId());
        String projectId = resolveProjectId(command.projectId(), scope);
        String conversationId = resolveConversationId(scope, command.conversationId());
        String now = now();
        MemorySuggestionRecord record = memorySuggestionRepository.save(new MemorySuggestionRecord(
                "memory-suggestion-" + UUID.randomUUID(),
                scope.wireValue(),
                projectId,
                conversationId,
                requireText(command.content(), "content"),
                MemorySuggestionStatus.PENDING.wireValue(),
                now,
                now
        ));
        MemorySuggestionView suggestion = record.toView();
        eventPublisher.publishEvent(new MemorySuggestionPublishedEvent(projectId, conversationId, suggestion));
        return suggestion;
    }

    public MemorySuggestionView acceptSuggestion(String projectId, String suggestionId) {
        MemorySuggestionRecord current = requireSuggestion(projectId, suggestionId);
        if (MemorySuggestionStatus.from(current.status()) == MemorySuggestionStatus.ACCEPTED) {
            return current.toView();
        }
        if (MemorySuggestionStatus.from(current.status()) == MemorySuggestionStatus.REJECTED) {
            throw new ApiException(HttpStatus.CONFLICT, "MEMORY_SUGGESTION_REJECTED", "Rejected suggestions cannot be accepted");
        }
        MemorySuggestionRecord updated = memorySuggestionRepository.updateStatus(current.id(), MemorySuggestionStatus.ACCEPTED, now());
        memoryItemRepository.save(new MemoryItemRecord(
                "memory-item-" + UUID.randomUUID(),
                current.scope(),
                current.projectId(),
                current.conversationId(),
                current.content(),
                MemorySource.ASSISTED.wireValue(),
                now()
        ));
        return updated.toView();
    }

    public MemorySuggestionView rejectSuggestion(String projectId, String suggestionId) {
        MemorySuggestionRecord current = requireSuggestion(projectId, suggestionId);
        if (MemorySuggestionStatus.from(current.status()) == MemorySuggestionStatus.REJECTED) {
            return current.toView();
        }
        if (MemorySuggestionStatus.from(current.status()) == MemorySuggestionStatus.ACCEPTED) {
            throw new ApiException(HttpStatus.CONFLICT, "MEMORY_SUGGESTION_ACCEPTED", "Accepted suggestions cannot be rejected");
        }
        return memorySuggestionRepository.updateStatus(current.id(), MemorySuggestionStatus.REJECTED, now()).toView();
    }

    private MemorySuggestionRecord requireSuggestion(String projectId, String suggestionId) {
        MemorySuggestionRecord suggestion = memorySuggestionRepository.findById(requireText(suggestionId, "suggestionId"))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "MEMORY_SUGGESTION_NOT_FOUND", "Memory suggestion does not exist"));
        if (suggestion.projectId() != null && !projectId.equals(suggestion.projectId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "MEMORY_SUGGESTION_NOT_FOUND", "Memory suggestion does not exist in this project");
        }
        return suggestion;
    }

    private boolean matchesScope(String scopeFilter, String conversationId, String projectId, MemoryItemView item) {
        String normalized = scopeFilter == null ? "" : scopeFilter.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "conversation" -> StringUtils.hasText(conversationId) && conversationId.equals(item.conversationId());
            case "global" -> "global".equals(item.scope());
            case "all" -> true;
            default -> "project".equals(item.scope()) || "global".equals(item.scope()) || projectId.equals(item.projectId());
        };
    }

    private boolean matchesSuggestionScope(String scopeFilter, String conversationId, String projectId, MemorySuggestionView suggestion) {
        String normalized = scopeFilter == null ? "" : scopeFilter.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "conversation" -> StringUtils.hasText(conversationId) && suggestion.scope().equals("conversation");
            case "global" -> "global".equals(suggestion.scope());
            case "all" -> true;
            default -> "project".equals(suggestion.scope()) || "global".equals(suggestion.scope()) || projectId.equals(suggestion.projectId());
        };
    }

    private MemoryScope resolveScope(String scope, String conversationId) {
        if (!StringUtils.hasText(scope) && StringUtils.hasText(conversationId)) {
            return MemoryScope.CONVERSATION;
        }
        MemoryScope resolved = MemoryScope.from(scope);
        if (resolved == MemoryScope.CONVERSATION && !StringUtils.hasText(conversationId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MEMORY_CONVERSATION_REQUIRED", "Conversation id is required for conversation-scoped memory");
        }
        return resolved;
    }

    private String resolveProjectId(String projectId, MemoryScope scope) {
        if (scope == MemoryScope.GLOBAL) {
            return null;
        }
        return requireText(projectId, "projectId");
    }

    private String resolveProjectId(String projectId, MemoryScope scope, String fallbackProjectId) {
        String resolved = resolveProjectId(projectId, scope);
        return resolved == null ? fallbackProjectId : resolved;
    }

    private String resolveConversationId(MemoryScope scope, String conversationId) {
        if (scope == MemoryScope.CONVERSATION) {
            return requireText(conversationId, "conversationId");
        }
        return StringUtils.hasText(conversationId) ? conversationId.trim() : null;
    }

    private String requireText(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, field.toUpperCase(Locale.ROOT) + "_REQUIRED", field + " is required");
        }
        return value.trim();
    }

    private <T> CursorPage<T> page(List<T> items, String cursor, Integer limit) {
        int offset = parseCursor(cursor);
        int pageSize = limit == null || limit <= 0 ? DEFAULT_LIMIT : limit;
        if (offset >= items.size()) {
            return new CursorPage<>(List.of(), null, false);
        }
        List<T> page = items.subList(offset, Math.min(items.size(), offset + pageSize));
        boolean hasMore = offset + page.size() < items.size();
        String nextCursor = hasMore ? String.valueOf(offset + page.size()) : null;
        return new CursorPage<>(List.copyOf(page), nextCursor, hasMore);
    }

    private int parseCursor(String cursor) {
        if (!StringUtils.hasText(cursor)) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(cursor.trim()));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private String now() {
        return Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
    }
}
