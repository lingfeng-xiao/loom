package com.loom.server.dto;

import jakarta.validation.constraints.NotBlank;

public record ProjectSwitchRequest(@NotBlank String projectId) {
}
