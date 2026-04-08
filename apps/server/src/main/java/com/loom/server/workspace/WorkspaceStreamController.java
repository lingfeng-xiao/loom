package com.loom.server.workspace;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class WorkspaceStreamController {

    private final WorkspaceStateService workspaceStateService;

    public WorkspaceStreamController(WorkspaceStateService workspaceStateService) {
        this.workspaceStateService = workspaceStateService;
    }

    @GetMapping(path = "/api/projects/{projectId}/conversations/{conversationId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamConversation(@PathVariable String projectId, @PathVariable String conversationId) throws Exception {
        SseEmitter emitter = new SseEmitter(5_000L);
        for (var event : workspaceStateService.getStreamEvents(projectId, conversationId)) {
            emitter.send(SseEmitter.event().name(String.valueOf(event.get("event"))).data(event));
        }
        emitter.complete();
        return emitter;
    }
}
