package com.anchoriq.ai.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * OpenClaw /v1/chat/completions 응답 DTO (OpenAI 호환 형식).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenClawResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("object")
    private String object;

    @JsonProperty("choices")
    private List<Choice> choices;

    @JsonProperty("usage")
    private Usage usage;

    public String getContent() {
        if (choices == null || choices.isEmpty()) {
            return "";
        }
        return choices.get(0).getMessage().getContent();
    }

    public String getId() {
        return id;
    }

    public String getObject() {
        return object;
    }

    public int getTotalTokens() {
        return usage != null ? usage.getTotalTokens() : 0;
    }

    public List<Choice> getChoices() {
        return choices;
    }

    public Usage getUsage() {
        return usage;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {
        @JsonProperty("index")
        private int index;

        @JsonProperty("message")
        private Message message;

        @JsonProperty("finish_reason")
        private String finishReason;

        public int getIndex() {
            return index;
        }

        public Message getMessage() {
            return message;
        }

        public String getFinishReason() {
            return finishReason;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        @JsonProperty("role")
        private String role;

        @JsonProperty("content")
        private String content;

        public String getRole() {
            return role;
        }

        public String getContent() {
            return content;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Usage {
        @JsonProperty("total_tokens")
        private int totalTokens;

        @JsonProperty("prompt_tokens")
        private int promptTokens;

        @JsonProperty("completion_tokens")
        private int completionTokens;

        public int getTotalTokens() {
            return totalTokens;
        }

        public int getPromptTokens() {
            return promptTokens;
        }

        public int getCompletionTokens() {
            return completionTokens;
        }
    }
}
