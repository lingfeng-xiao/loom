package com.loom.server.dto;

public record AppearanceSettingsUpdateRequest(
        String theme,
        String density,
        String accentTone,
        String fontSans,
        String fontMono
) {
}
