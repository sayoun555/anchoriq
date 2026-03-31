package com.anchoriq.ai.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

/**
 * OpenClaw /v1/chat/completions 요청 DTO (OpenAI 호환 형식).
 */
public class OpenClawRequest {

    private final String model;
    private final List<ChatMessage> messages;
    private final double temperature;

    @JsonProperty("max_tokens")
    private final int maxTokens;

    private OpenClawRequest(String model, List<ChatMessage> messages,
                            double temperature, int maxTokens) {
        this.model = Objects.requireNonNull(model);
        this.messages = Objects.requireNonNull(messages);
        this.temperature = temperature;
        this.maxTokens = maxTokens;
    }

    public static OpenClawRequest of(String systemPrompt, String userMessage,
                                      String model, double temperature, int maxTokens) {
        List<ChatMessage> messages = List.of(
                new ChatMessage("system", systemPrompt),
                new ChatMessage("user", userMessage)
        );
        return new OpenClawRequest(model, messages, temperature, maxTokens);
    }

    public String getModel() {
        return model;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public double getTemperature() {
        return temperature;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    /**
     * Chat message with role and content.
     */
    public static class ChatMessage {
        private final String role;
        private final String content;

        public ChatMessage(String role, String content) {
            this.role = Objects.requireNonNull(role);
            this.content = Objects.requireNonNull(content);
        }

        public String getRole() {
            return role;
        }

        public String getContent() {
            return content;
        }
    }
}
