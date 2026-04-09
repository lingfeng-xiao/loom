package com.loom.server.workspace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loom.server.api.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MinimaxChatClient {
    private static final String OPEN_THINK_TAG = "<think>";
    private static final String CLOSE_THINK_TAG = "</think>";
    private static final Pattern THINK_BLOCK_PATTERN = Pattern.compile("<think>(.*?)</think>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public MinimaxChatClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public ChatCompletionResult complete(ChatCompletionRequest request) {
        long startedAt = System.currentTimeMillis();
        HttpRequest httpRequest = buildRequest(request, false);

        HttpResponse<String> response;
        try {
            response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.BAD_GATEWAY, "LLM_REQUEST_FAILED", "Failed to reach the configured LLM endpoint");
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "LLM_REQUEST_FAILED", "Failed to reach the configured LLM endpoint");
        }

        long latencyMs = System.currentTimeMillis() - startedAt;
        JsonNode payload;
        try {
            payload = objectMapper.readTree(response.body());
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "LLM_RESPONSE_INVALID", "The configured LLM endpoint returned invalid JSON");
        }

        if (response.statusCode() >= 400) {
            String message = payload.path("error").path("message").asText();
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "LLM_REQUEST_REJECTED",
                    message.isBlank() ? "The configured LLM endpoint rejected the request" : message
            );
        }

        JsonNode choice = payload.path("choices").path(0);
        JsonNode messageNode = choice.path("message");
        String outputText = contentToText(messageNode.path("content"));
        if (outputText.isBlank()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "LLM_RESPONSE_EMPTY", "The configured LLM endpoint returned an empty response");
        }

        String reasoningSummary = extractReasoningSummary(outputText);
        String assistantText = stripThinkBlocks(outputText);
        if (assistantText.isBlank()) {
            assistantText = outputText;
        }

        return new ChatCompletionResult(
                assistantText,
                summarize(reasoningSummary.isBlank() ? assistantText : reasoningSummary),
                payload.path("model").asText(request.modelId()),
                payload.path("id").asText(null),
                latencyMs
        );
    }

    public ChatCompletionResult stream(ChatCompletionRequest request, StreamListener listener) {
        long startedAt = System.currentTimeMillis();
        HttpRequest httpRequest = buildRequest(request, true);

        HttpResponse<InputStream> response;
        try {
            response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.BAD_GATEWAY, "LLM_REQUEST_FAILED", "Failed to reach the configured LLM endpoint");
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "LLM_REQUEST_FAILED", "Failed to reach the configured LLM endpoint");
        }

        if (response.statusCode() >= 400) {
            try (InputStream errorStream = response.body()) {
                JsonNode payload = objectMapper.readTree(errorStream.readAllBytes());
                String message = payload.path("error").path("message").asText();
                throw new ApiException(
                        HttpStatus.BAD_GATEWAY,
                        "LLM_REQUEST_REJECTED",
                        message.isBlank() ? "The configured LLM endpoint rejected the request" : message
                );
            } catch (IOException exception) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "LLM_RESPONSE_INVALID", "The configured LLM endpoint returned invalid JSON");
            }
        }

        StringBuilder rawAssistantBuilder = new StringBuilder();
        StringBuilder visibleAssistantBuilder = new StringBuilder();
        StringBuilder reasoningBuilder = new StringBuilder();
        String responseModel = request.modelId();
        String requestId = null;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || !line.startsWith("data:")) {
                    continue;
                }

                String data = line.substring(5).trim();
                if ("[DONE]".equals(data)) {
                    break;
                }

                JsonNode payload = objectMapper.readTree(data);
                if (payload.path("error").isObject()) {
                    String message = payload.path("error").path("message").asText();
                    throw new ApiException(HttpStatus.BAD_GATEWAY, "LLM_REQUEST_REJECTED", message.isBlank() ? "The configured LLM endpoint rejected the request" : message);
                }

                responseModel = payload.path("model").asText(responseModel);
                requestId = payload.path("id").asText(requestId);

                JsonNode delta = payload.path("choices").path(0).path("delta");
                if (delta.isMissingNode() || delta.isNull()) {
                    continue;
                }

                String reasoningChunk = extractReasoningDelta(delta, reasoningBuilder.toString());
                if (!reasoningChunk.isBlank()) {
                    reasoningBuilder.append(reasoningChunk);
                    listener.onReasoningDelta(reasoningChunk);
                }

                String rawAssistantChunk = contentToText(delta.path("content"));
                if (!rawAssistantChunk.isBlank()) {
                    rawAssistantBuilder.append(rawAssistantChunk);
                }

                String assistantChunk = normalizeStreamDelta(
                        visibleAssistantBuilder.toString(),
                        visibleAssistantText(rawAssistantBuilder.toString())
                );
                if (!assistantChunk.isBlank()) {
                    visibleAssistantBuilder.append(assistantChunk);
                    listener.onTextDelta(assistantChunk);
                }
            }
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "LLM_RESPONSE_INVALID", "The configured LLM endpoint returned invalid stream data");
        }

        String assistantText = visibleAssistantBuilder.toString().trim();
        if (assistantText.isBlank()) {
            assistantText = stripThinkBlocks(rawAssistantBuilder.toString()).trim();
        }
        String reasoningSummary = reasoningBuilder.toString().trim();
        if (reasoningSummary.isBlank()) {
            reasoningSummary = extractReasoningSummary(rawAssistantBuilder.toString());
        }
        if (assistantText.isBlank()) {
            assistantText = stripThinkBlocks(reasoningSummary).trim();
        }
        if (assistantText.isBlank()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "LLM_RESPONSE_EMPTY", "The configured LLM endpoint returned an empty streamed response");
        }

        return new ChatCompletionResult(
                assistantText,
                summarize(reasoningSummary.isBlank() ? assistantText : reasoningSummary),
                responseModel,
                requestId,
                System.currentTimeMillis() - startedAt
        );
    }

    private HttpRequest buildRequest(ChatCompletionRequest request, boolean stream) {
        try {
            return HttpRequest.newBuilder()
                    .uri(URI.create(joinPath(request.apiBaseUrl(), "/chat/completions")))
                    .timeout(Duration.ofMillis(request.timeoutMs()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + request.apiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(toPayload(request, stream))))
                    .build();
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "LLM_PAYLOAD_SERIALIZATION_FAILED", "Failed to serialize LLM request");
        }
    }

    private JsonNode toPayload(ChatCompletionRequest request, boolean stream) {
        var root = objectMapper.createObjectNode();
        root.put("model", request.modelId());
        root.put("temperature", normalizeTemperature(request.temperature()));
        root.put("stream", stream);
        if (request.maxTokens() != null) {
            root.put("max_tokens", request.maxTokens());
        }
        if (stream && request.apiBaseUrl().toLowerCase().contains("minimax")) {
            root.putObject("extra_body").put("reasoning_split", true);
        }

        var messagesNode = root.putArray("messages");
        for (ChatMessage message : request.messages()) {
            var messageNode = messagesNode.addObject();
            messageNode.put("role", message.role());
            messageNode.put("content", message.content());
        }
        return root;
    }

    private static String joinPath(String baseUrl, String suffix) {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + suffix;
        }
        return baseUrl + suffix;
    }

    private static String contentToText(JsonNode contentNode) {
        if (contentNode == null || contentNode.isMissingNode() || contentNode.isNull()) {
            return "";
        }
        if (contentNode.isTextual()) {
            return contentNode.asText();
        }
        if (contentNode.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode item : contentNode) {
                if (item.isTextual()) {
                    builder.append(item.asText());
                    continue;
                }
                JsonNode textNode = item.path("text");
                if (textNode.isTextual()) {
                    builder.append(textNode.asText());
                }
            }
            return builder.toString();
        }
        return "";
    }

    private static String extractReasoningDelta(JsonNode deltaNode, String existingReasoning) {
        String directReasoning = contentToText(deltaNode.path("reasoning_content"));
        if (directReasoning.isBlank()) {
            directReasoning = contentToText(deltaNode.path("reasoning"));
        }
        if (directReasoning.isBlank()) {
            directReasoning = reasoningDetailsToText(deltaNode.path("reasoning_details"));
        }
        return normalizeStreamDelta(existingReasoning, directReasoning);
    }

    private static String reasoningDetailsToText(JsonNode reasoningDetailsNode) {
        if (reasoningDetailsNode == null || reasoningDetailsNode.isMissingNode() || reasoningDetailsNode.isNull()) {
            return "";
        }
        if (reasoningDetailsNode.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode item : reasoningDetailsNode) {
                String text = item.path("text").asText();
                if (!text.isBlank()) {
                    builder.append(text);
                }
                String summary = item.path("summary").asText();
                if (!summary.isBlank()) {
                    builder.append(summary);
                }
            }
            return builder.toString();
        }
        return reasoningDetailsNode.path("text").asText("");
    }

    private static String normalizeStreamDelta(String existing, String incoming) {
        if (incoming == null || incoming.isBlank()) {
            return "";
        }
        if (existing.isBlank()) {
            return incoming;
        }
        if (incoming.startsWith(existing)) {
            return incoming.substring(existing.length());
        }
        if (existing.endsWith(incoming)) {
            return "";
        }
        return incoming;
    }

    private static String visibleAssistantText(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return "";
        }

        StringBuilder visible = new StringBuilder(rawText.length());
        boolean insideThink = false;
        int index = 0;

        while (index < rawText.length()) {
            if (!insideThink) {
                if (startsWithIgnoreCase(rawText, index, OPEN_THINK_TAG)) {
                    insideThink = true;
                    index += OPEN_THINK_TAG.length();
                    continue;
                }
                if (isPartialTagPrefix(rawText, index, OPEN_THINK_TAG)) {
                    break;
                }
                visible.append(rawText.charAt(index));
                index++;
                continue;
            }

            if (startsWithIgnoreCase(rawText, index, CLOSE_THINK_TAG)) {
                insideThink = false;
                index += CLOSE_THINK_TAG.length();
                continue;
            }
            if (isPartialTagPrefix(rawText, index, CLOSE_THINK_TAG)) {
                break;
            }
            index++;
        }

        return visible.toString();
    }

    private static boolean startsWithIgnoreCase(String text, int offset, String expected) {
        return offset + expected.length() <= text.length() && text.regionMatches(true, offset, expected, 0, expected.length());
    }

    private static boolean isPartialTagPrefix(String text, int offset, String tag) {
        int remaining = text.length() - offset;
        if (remaining <= 0 || remaining >= tag.length()) {
            return false;
        }
        return tag.regionMatches(true, 0, text, offset, remaining);
    }

    private static String summarize(String outputText) {
        if (outputText.length() <= 96) {
            return outputText;
        }
        return outputText.substring(0, 95) + "...";
    }

    private static double normalizeTemperature(double value) {
        if (value <= 0d || value > 1d) {
            return 1.0d;
        }
        return value;
    }

    private static String stripThinkBlocks(String outputText) {
        return THINK_BLOCK_PATTERN.matcher(outputText).replaceAll("").trim();
    }

    private static String extractReasoningSummary(String outputText) {
        Matcher matcher = THINK_BLOCK_PATTERN.matcher(outputText);
        if (!matcher.find()) {
            return "";
        }
        return matcher.group(1).trim();
    }

    public record ChatMessage(String role, String content) {
    }

    public record ChatCompletionRequest(
            String apiBaseUrl,
            String apiKey,
            String modelId,
            double temperature,
            Integer maxTokens,
            int timeoutMs,
            List<ChatMessage> messages
    ) {
    }

    public record ChatCompletionResult(
            String outputText,
            String reasoningSummary,
            String responseModel,
            String requestId,
            long latencyMs
    ) {
    }

    public interface StreamListener {
        void onReasoningDelta(String delta);

        void onTextDelta(String delta);
    }
}
