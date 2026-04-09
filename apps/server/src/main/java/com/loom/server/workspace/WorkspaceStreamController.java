package com.loom.server.workspace;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@RestController
public class WorkspaceStreamController {

    private final WorkspaceStateService workspaceStateService;

    public WorkspaceStreamController(WorkspaceStateService workspaceStateService) {
        this.workspaceStateService = workspaceStateService;
    }

    @GetMapping(path = "/api/projects/{projectId}/conversations/{conversationId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamConversation(@PathVariable String projectId, @PathVariable String conversationId) {
        SseEmitter emitter = new SseEmitter(120_000L);
        CompletableFuture.runAsync(() -> {
            try {
                workspaceStateService.streamConversation(projectId, conversationId, event -> sendEvent(emitter, event));
                emitter.complete();
            } catch (Exception exception) {
                emitter.completeWithError(exception);
            }
        });
        return emitter;
    }

    private void sendEvent(SseEmitter emitter, java.util.Map<String, Object> event) {
        try {
            emitter.send(SseEmitter.event().name(String.valueOf(event.get("event"))).data(event));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write the SSE event", exception);
        }
    }
}
