package com.loom.server.dto;

import java.util.List;

public record PlanCompleteRequest(
        String summary,
        List<String> outputAssetIds,
        List<String> logs
) {
}
