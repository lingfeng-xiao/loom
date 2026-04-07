package com.loom.server.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record AssetFromPlanRequest(
        @NotBlank String planId,
        @NotBlank String title,
        @NotBlank String content,
        List<String> tags
) {
}
