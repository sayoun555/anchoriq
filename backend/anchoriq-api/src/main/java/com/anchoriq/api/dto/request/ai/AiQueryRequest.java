package com.anchoriq.api.dto.request.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiQueryRequest(
        @NotBlank(message = "Query must not be blank")
        @Size(max = 500, message = "Query must be under 500 characters")
        String query
) {
}
