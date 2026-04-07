package com.loom.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loom.server.config.LoomProperties;
import com.loom.server.model.Conversation;
import com.loom.server.model.Message;
import com.loom.server.model.ModelSettings;
import com.loom.server.model.Project;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenAiCompatibleLlmGateway implements LlmGateway {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    private final ObjectMapper objectMapper;
    private final LoomProperties properties;
    private final HttpClient httpClient;

    public OpenAiCompatibleLlmGateway(ObjectMapper objectMapper, LoomProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    @Override
    public String complete(Project project, Conversation conversation, List<Message> messages, ModelSettings settings) {
        String baseUrl = resolveBaseUrl(settings);
        String model = resolveModel(settings);
        String payload = buildPayload(project, conversation, messages, settings, model);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload));

        String apiKey = properties.getAi().getApiKey();
        if (StringUtils.hasText(apiKey)) {
            requestBuilder.header("Authorization", "Bearer " + apiKey.trim());
        }

        try {
            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new LlmGatewayException("LLM request failed with status " + response.statusCode() + ": " + trimBody(response.body()));
            }

            String content = extractAssistantContent(response.body());
            if (!StringUtils.hasText(content)) {
                throw new LlmGatewayException("LLM response did not include assistant content");
            }
            return content.trim();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new LlmGatewayException("LLM request was interrupted", exception);
        } catch (IOException exception) {
            throw new LlmGatewayException("LLM request failed: " + exception.getMessage(), exception);
        }
    }

    private String buildPayload(
            Project project,
            Conversation conversation,
            List<Message> messages,
            ModelSettings settings,
            String model
    ) {
        List<Map<String, Object>> promptMessages = new ArrayList<>();
        promptMessages.add(Map.of(
                "role", "system",
                "content", buildSystemPrompt(project, conversation)
        ));

        for (Message message : messages) {
            String role = normalizeRole(message.role());
            if (role == null || !StringUtils.hasText(message.content())) {
                continue;
            }
            promptMessages.add(Map.of(
                    "role", role,
                    "content", message.content()
            ));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("temperature", settings.temperature());
        payload.put("messages", promptMessages);

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new LlmGatewayException("Failed to serialize LLM request payload", exception);
        }
    }

    private String buildSystemPrompt(Project project, Conversation conversation) {
        StringJoiner joiner = new StringJoiner("\n");
        joiner.add(properties.getAi().getSystemPrompt().trim());
        joiner.add("");
        joiner.add("Current workspace context:");
        joiner.add("- project_name: " + project.name());
        joiner.add("- project_type: " + project.type().value());
        joiner.add("- conversation_mode: " + conversation.mode().value());
        joiner.add("- knowledge_roots: " + joinValues(project.knowledgeRoots()));
        if (StringUtils.hasText(project.description())) {
            joiner.add("- project_description: " + project.description().trim());
        }
        return joiner.toString();
    }

    private String extractAssistantContent(String responseBody) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode contentNode = root.at("/choices/0/message/content");
        if (contentNode.isTextual()) {
            return contentNode.asText();
        }
        if (contentNode.isArray()) {
            StringJoiner joiner = new StringJoiner("");
            for (JsonNode item : contentNode) {
                JsonNode textNode = item.path("text");
                if (textNode.isTextual()) {
                    joiner.add(textNode.asText());
                }
            }
            return joiner.toString();
        }

        JsonNode fallbackText = root.at("/choices/0/text");
        if (fallbackText.isTextual()) {
            return fallbackText.asText();
        }
        return "";
    }

    private String resolveBaseUrl(ModelSettings settings) {
        String baseUrl = StringUtils.hasText(settings.baseUrl()) ? settings.baseUrl().trim() : properties.getAi().getBaseUrl().trim();
        if (!StringUtils.hasText(baseUrl)) {
            throw new LlmGatewayException("LLM base URL is not configured");
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private String resolveModel(ModelSettings settings) {
        String model = StringUtils.hasText(settings.model()) ? settings.model().trim() : properties.getAi().getModel().trim();
        if (!StringUtils.hasText(model)) {
            throw new LlmGatewayException("LLM model is not configured");
        }
        return model;
    }

    private String normalizeRole(String rawRole) {
        if (!StringUtils.hasText(rawRole)) {
            return null;
        }
        String role = rawRole.trim().toLowerCase(Locale.ROOT);
        return switch (role) {
            case "user", "assistant" -> role;
            default -> null;
        };
    }

    private String joinValues(List<String> values) {
        return values == null || values.isEmpty() ? "none" : String.join(", ", values);
    }

    private String trimBody(String body) {
        if (!StringUtils.hasText(body)) {
            return "<empty>";
        }
        String value = body.replaceAll("\\s+", " ").trim();
        return value.length() <= 240 ? value : value.substring(0, 240) + "...";
    }
}
