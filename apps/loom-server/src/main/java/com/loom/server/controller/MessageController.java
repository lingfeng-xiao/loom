package com.loom.server.controller;

import com.loom.server.api.ApiEnvelope;
import com.loom.server.dto.MessageCreateRequest;
import com.loom.server.model.Message;
import com.loom.server.service.ConversationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final ConversationService conversationService;

    public MessageController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping
    public ApiEnvelope<List<Message>> list(@RequestParam String conversationId) {
        return ApiEnvelope.of(conversationService.listMessages(conversationId));
    }

    @PostMapping("/{conversationId}")
    public ApiEnvelope<Message> add(@PathVariable String conversationId, @Valid @RequestBody MessageCreateRequest request) {
        return ApiEnvelope.of(conversationService.addMessage(conversationId, request));
    }
}
