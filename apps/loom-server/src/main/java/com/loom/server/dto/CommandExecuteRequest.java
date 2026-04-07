package com.loom.server.dto;

import com.loom.server.model.CommandId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record CommandExecuteRequest(
        @NotBlank String projectId,
        String conversationId,
        @NotNull CommandId commandId,
        Map<String, String> args
) {
}
