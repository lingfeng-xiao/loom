package com.loom.server.service;

import com.loom.server.dto.ConversationCreateRequest;
import com.loom.server.dto.ConversationUpdateRequest;
import com.loom.server.dto.MessageCreateRequest;
import com.loom.server.error.NotFoundException;
import com.loom.server.model.Conversation;
import com.loom.server.model.ConversationMode;
import com.loom.server.model.ConversationStatus;
import com.loom.server.model.Message;
import com.loom.server.model.Project;
import com.loom.server.repository.ConversationRepository;
import com.loom.server.repository.MessageRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ProjectService projectService;
    private final AuditService auditService;
    private final WorkspaceSettingsService workspaceSettingsService;
    private final LlmGateway llmGateway;

    public ConversationService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            ProjectService projectService,
            AuditService auditService,
            WorkspaceSettingsService workspaceSettingsService,
            LlmGateway llmGateway
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.projectService = projectService;
        this.auditService = auditService;
        this.workspaceSettingsService = workspaceSettingsService;
        this.llmGateway = llmGateway;
    }

    public List<Conversation> listConversations(String projectId) {
        return listConversations(projectId, null, null, null);
    }

    public List<Conversation> listConversations(String projectId, String modeValue, String statusValue, String query) {
        return conversationRepository.findByProject(projectId, modeValue, statusValue, query);
    }

    public Conversation createConversation(String projectId, ConversationCreateRequest request) {
        projectService.getProject(projectId);
        Instant now = Instant.now();
        Conversation conversation = new Conversation(
                UUID.randomUUID().toString(),
                projectId,
                request.title(),
                request.mode() == null ? ConversationMode.CHAT : request.mode(),
                ConversationStatus.ACTIVE,
                request.summary() == null ? "" : request.summary(),
                now,
                now
        );
        conversationRepository.save(conversation);
        auditService.record("system", "conversation-service", "conversation", conversation.id(), "create", request.title(), "ok");
        return conversation;
    }

    public Conversation getConversation(String id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("未找到会话 " + id));
    }

    public Conversation updateConversation(String conversationId, ConversationUpdateRequest request) {
        Conversation current = getConversation(conversationId);
        Conversation updated = new Conversation(
                current.id(),
                current.projectId(),
                request.title() == null || request.title().isBlank() ? current.title() : request.title(),
                current.mode(),
                request.status() == null ? current.status() : request.status(),
                request.summary() == null ? current.summary() : request.summary(),
                current.createdAt(),
                Instant.now()
        );
        conversationRepository.save(updated);
        auditService.record("system", "conversation-service", "conversation", updated.id(), "update", updated.title(), "ok");
        return updated;
    }

    public Message addMessage(String conversationId, MessageCreateRequest request) {
        Conversation conversation = getConversation(conversationId);
        String role = request.role().trim().toLowerCase();
        Message message = new Message(
                UUID.randomUUID().toString(),
                conversation.id(),
                conversation.projectId(),
                role,
                request.content(),
                Instant.now()
        );
        messageRepository.save(message);
        maybeAppendAssistantReply(conversation, message);

        Conversation updatedConversation = new Conversation(
                conversation.id(),
                conversation.projectId(),
                conversation.title(),
                conversation.mode(),
                conversation.status(),
                conversation.summary().isBlank() ? truncate(request.content()) : conversation.summary(),
                conversation.createdAt(),
                Instant.now()
        );
        conversationRepository.save(updatedConversation);
        auditService.record("system", "conversation-service", "message", message.id(), "append", request.content(), "ok");
        return message;
    }

    public List<Message> listMessages(String conversationId) {
        return messageRepository.findByConversation(conversationId);
    }

    private String truncate(String value) {
        return value.length() <= 120 ? value : value.substring(0, 120);
    }

    private void maybeAppendAssistantReply(Conversation conversation, Message userMessage) {
        if (!"user".equals(userMessage.role())) {
            return;
        }

        Project project = projectService.getProject(conversation.projectId());
        try {
            String assistantReply = llmGateway.complete(
                    project,
                    conversation,
                    messageRepository.findByConversation(conversation.id()),
                    workspaceSettingsService.getSettings().model()
            );
            Message assistantMessage = new Message(
                    UUID.randomUUID().toString(),
                    conversation.id(),
                    conversation.projectId(),
                    "assistant",
                    assistantReply,
                    Instant.now()
            );
            messageRepository.save(assistantMessage);
            auditService.record("system", "conversation-service", "message", assistantMessage.id(), "generate", assistantReply, "ok");
        } catch (LlmGatewayException exception) {
            String content = "LLM 回复失败，请检查 Base URL、Model 和服务端 API Key 配置。原因: " + exception.getMessage();
            Message systemMessage = new Message(
                    UUID.randomUUID().toString(),
                    conversation.id(),
                    conversation.projectId(),
                    "system",
                    content,
                    Instant.now()
            );
            messageRepository.save(systemMessage);
            auditService.record("system", "conversation-service", "message", systemMessage.id(), "generate-error", content, "failed");
        }
    }
}
