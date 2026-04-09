package com.loom.server.api;

import java.util.Map;

public record ApiErrorEnvelope(ApiError error, Map<String, Object> meta) {

    public static ApiErrorEnvelope of(String code, String message) {
        return new ApiErrorEnvelope(new ApiError(code, message, null, null), Map.of());
    }

    public record ApiError(String code, String message, Boolean retryable, Object fieldErrors) {
    }
}
