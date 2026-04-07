package com.loom.server.model;

public record AppearanceSettings(
        String theme,
        String density,
        String accentTone,
        String fontSans,
        String fontMono
) {
}
