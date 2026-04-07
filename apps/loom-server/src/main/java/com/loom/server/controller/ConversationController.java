package com.loom.server.controller;

import com.loom.server.api.ApiEnvelope;
import com.loom.server.dto.ConversationCreateRequest;
import com.loom.server.dto.ConversationUpdateRequest;
import com.loom.server.model.Conversation;
import com.loom.server.service.ConversationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping("/projects/{projectId}/conversations")
    public ApiEnvelope<List<Conversation>> listByProject(
            @PathVariable String projectId,
            @RequestParam(required = false) String mode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, name = "q") String query
    ) {
        return ApiEnvelope.of(conversationService.listConversations(projectId, mode, status, query));
    }

    @PostMapping("/projects/{projectId}/conversations")
    public ApiEnvelope<Conversation> create(@PathVariable String projectId, @Valid @RequestBody ConversationCreateRequest request) {
        return ApiEnvelope.of(conversationService.createConversation(projectId, request));
    }

    @GetMapping("/conversations/{conversationId}")
    public ApiEnvelope<Conversation> get(@PathVariable String conversationId) {
        return ApiEnvelope.of(conversationService.getConversation(conversationId));
    }

    @PatchMapping("/conversations/{conversationId}")
    public ApiEnvelope<Conversation> update(@PathVariable String conversationId, @RequestBody ConversationUpdateRequest request) {
        return ApiEnvelope.of(conversationService.updateConversation(conversationId, request));
    }
}
