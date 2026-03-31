package com.anchoriq.api.dto.request.ai;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RouteCompareRequest(
        @NotEmpty(message = "Route IDs must not be empty")
        @Size(min = 2, max = 5, message = "Must compare between 2 and 5 routes")
        List<Long> routeIds
) {
}
