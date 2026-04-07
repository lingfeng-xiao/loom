package com.loom.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.loom.server.dto.MessageCreateRequest;
import com.loom.server.model.CommandId;
import com.loom.server.model.Conversation;
import com.loom.server.model.ConversationMode;
import com.loom.server.model.ConversationStatus;
import com.loom.server.model.Message;
import com.loom.server.model.ModelSettings;
import com.loom.server.model.Project;
import com.loom.server.model.ProjectType;
import com.loom.server.model.SkillId;
import com.loom.server.model.WorkspaceSettings;
import com.loom.server.repository.ConversationRepository;
import com.loom.server.repository.MessageRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ProjectService projectService;

    @Mock
    private AuditService auditService;

    @Mock
    private WorkspaceSettingsService workspaceSettingsService;

    @Mock
    private LlmGateway llmGateway;

    @Test
    void appendsAssistantReplyForUserMessages() {
        Conversation conversationServiceRecord = conversation();
        Project project = project();
        WorkspaceSettings workspaceSettings = workspaceSettings();

        when(conversationRepository.findById(conversationServiceRecord.id())).thenReturn(Optional.of(conversationServiceRecord));
        when(projectService.getProject(project.id())).thenReturn(project);
        when(workspaceSettingsService.getSettings()).thenReturn(workspaceSettings);
        when(messageRepository.findByConversation(conversationServiceRecord.id())).thenReturn(List.of(
                new Message("user-1", conversationServiceRecord.id(), project.id(), "user", "Hello Loom", Instant.now())
        ));
        when(llmGateway.complete(any(), any(), any(), any())).thenReturn("Hi, I'm connected.");

        ConversationService service = new ConversationService(
                conversationRepository,
                messageRepository,
                projectService,
                auditService,
                workspaceSettingsService,
                llmGateway
        );

        Message result = service.addMessage(conversationServiceRecord.id(), new MessageCreateRequest("user", "Hello Loom"));

        assertThat(result.role()).isEqualTo("user");
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository, times(2)).save(messageCaptor.capture());
        assertThat(messageCaptor.getAllValues()).extracting(Message::role).containsExactly("user", "assistant");
        assertThat(messageCaptor.getAllValues().get(1).content()).isEqualTo("Hi, I'm connected.");
    }

    @Test
    void appendsSystemMessageWhenAssistantGenerationFails() {
        Conversation conversationServiceRecord = conversation();
        Project project = project();
        WorkspaceSettings workspaceSettings = workspaceSettings();

        when(conversationRepository.findById(conversationServiceRecord.id())).thenReturn(Optional.of(conversationServiceRecord));
        when(projectService.getProject(project.id())).thenReturn(project);
        when(workspaceSettingsService.getSettings()).thenReturn(workspaceSettings);
        when(messageRepository.findByConversation(conversationServiceRecord.id())).thenReturn(List.of(
                new Message("user-1", conversationServiceRecord.id(), project.id(), "user", "Hello Loom", Instant.now())
        ));
        when(llmGateway.complete(any(), any(), any(), any())).thenThrow(new LlmGatewayException("401 Unauthorized"));

        ConversationService service = new ConversationService(
                conversationRepository,
                messageRepository,
                projectService,
                auditService,
                workspaceSettingsService,
                llmGateway
        );

        service.addMessage(conversationServiceRecord.id(), new MessageCreateRequest("user", "Hello Loom"));

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository, times(2)).save(messageCaptor.capture());
        assertThat(messageCaptor.getAllValues()).extracting(Message::role).containsExactly("user", "system");
        assertThat(messageCaptor.getAllValues().get(1).content()).contains("LLM 回复失败");
    }

    @Test
    void doesNotInvokeAssistantForNonUserMessages() {
        Conversation conversationServiceRecord = conversation();

        when(conversationRepository.findById(conversationServiceRecord.id())).thenReturn(Optional.of(conversationServiceRecord));

        ConversationService service = new ConversationService(
                conversationRepository,
                messageRepository,
                projectService,
                auditService,
                workspaceSettingsService,
                llmGateway
        );

        service.addMessage(conversationServiceRecord.id(), new MessageCreateRequest("system", "note"));

        verify(messageRepository, times(1)).save(any(Message.class));
        verify(llmGateway, times(0)).complete(any(), any(), any(), any());
    }

    private Conversation conversation() {
        Instant now = Instant.parse("2026-04-07T00:00:00Z");
        return new Conversation(
                "conversation-1",
                "project-1",
                "Test Conversation",
                ConversationMode.CHAT,
                ConversationStatus.ACTIVE,
                "",
                now,
                now
        );
    }

    private Project project() {
        Instant now = Instant.parse("2026-04-07T00:00:00Z");
        return new Project(
                "project-1",
                "Loom",
                ProjectType.KNOWLEDGE,
                "Test project",
                List.of(SkillId.KNOWLEDGE_CARD_GENERATOR),
                List.of(CommandId.PLAN),
                List.of(),
                List.of("docs"),
                List.of(),
                now,
                now
        );
    }

    private WorkspaceSettings workspaceSettings() {
        return new WorkspaceSettings(
                "Loom",
                "zh-CN",
                "comfortable",
                "project-1",
                "last_conversation",
                true,
                new ModelSettings("OpenAI Compatible", "http://127.0.0.1:11434/v1", "gpt-4.1-mini", 0.2),
                null,
                null,
                null,
                List.of(CommandId.PLAN),
                List.of(SkillId.KNOWLEDGE_CARD_GENERATOR),
                Instant.parse("2026-04-07T00:00:00Z")
        );
    }
}
