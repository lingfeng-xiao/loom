package com.template.server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record WorkspaceSettingsUpdateRequest(
        @NotBlank String workspaceName,
        @Email @NotBlank String supportEmail,
        @NotBlank String docsUrl,
        @Min(5) @Max(300) int defaultRefreshIntervalSeconds
) {
}
