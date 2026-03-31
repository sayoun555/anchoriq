package com.anchoriq.api.dto.request.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubscribeRequest(
        @NotBlank String plan,
        @NotNull String currency
) {}
