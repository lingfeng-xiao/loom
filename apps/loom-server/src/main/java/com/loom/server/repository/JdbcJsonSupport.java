package com.loom.server.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class JdbcJsonSupport {

    private final ObjectMapper objectMapper;

    public JdbcJsonSupport(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to write JSON payload", exception);
        }
    }

    public <T> T read(String json, TypeReference<T> typeReference, Supplier<T> defaultSupplier) {
        if (json == null || json.isBlank()) {
            return defaultSupplier.get();
        }
        try {
            return objectMapper.readValue(normalize(json), typeReference);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to read JSON payload", exception);
        }
    }

    public <T> T read(String json, Class<T> type, Supplier<T> defaultSupplier) {
        if (json == null || json.isBlank()) {
            return defaultSupplier.get();
        }
        try {
            return objectMapper.readValue(normalize(json), type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to read JSON payload", exception);
        }
    }

    private String normalize(String json) throws JsonProcessingException {
        JsonNode node = objectMapper.readTree(json);
        if (node != null && node.isTextual()) {
            return node.asText();
        }
        return json;
    }
}
