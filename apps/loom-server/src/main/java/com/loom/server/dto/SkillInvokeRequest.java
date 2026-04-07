package com.loom.server.dto;

import java.util.Map;

public record SkillInvokeRequest(
        String projectId,
        String conversationId,
        String planId,
        String nodeId,
        String input,
        Map<String, String> args
) {
}
