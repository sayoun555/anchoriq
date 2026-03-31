package com.anchoriq.api.dto.response.apikey;

import java.time.LocalDateTime;

public record ApiKeyResponse(
        Long id,
        String name,
        String keyPrefix,
        LocalDateTime lastUsedAt,
        LocalDateTime createdAt
) {}
