package com.loom.server.dto;

import com.loom.server.model.NodeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record NodeRegisterRequest(
        @NotBlank String name,
        @NotNull NodeType type,
        @NotBlank String host,
        List<String> tags,
        List<String> capabilities
) {
}
