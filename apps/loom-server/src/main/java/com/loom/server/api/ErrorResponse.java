package com.loom.server.api;

public record ErrorResponse(String code, String message, String details) {
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, null);
    }
}
