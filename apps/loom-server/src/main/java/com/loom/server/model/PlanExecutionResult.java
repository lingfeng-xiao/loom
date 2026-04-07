package com.loom.server.model;

import java.util.List;

public record PlanExecutionResult(
        String summary,
        List<String> outputAssetIds,
        List<String> logs
) {
}
