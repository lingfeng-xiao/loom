package com.loom.server.dto;

import jakarta.validation.constraints.NotBlank;

public record MessageCreateRequest(
        @NotBlank String role,
        @NotBlank String content
) {
}
