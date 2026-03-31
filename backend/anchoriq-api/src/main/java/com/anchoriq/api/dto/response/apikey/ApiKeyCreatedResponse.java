package com.anchoriq.api.dto.response.apikey;

public record ApiKeyCreatedResponse(
        String apiKey,
        String message
) {
    public static ApiKeyCreatedResponse of(String apiKey) {
        return new ApiKeyCreatedResponse(apiKey,
                "Save this key securely. It will not be shown again.");
    }
}
