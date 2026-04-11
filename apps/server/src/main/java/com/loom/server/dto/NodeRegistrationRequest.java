package com.loom.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record NodeRegistrationRequest(
        @NotBlank String name,
        @NotBlank String type,
        @NotBlank String host,
        @NotEmpty List<String> tags,
        List<String> capabilities
) {
}
