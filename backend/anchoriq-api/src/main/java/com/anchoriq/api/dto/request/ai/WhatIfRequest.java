package com.anchoriq.api.dto.request.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WhatIfRequest(
        @NotBlank(message = "Scenario must not be blank")
        @Size(max = 500, message = "Scenario must be under 500 characters")
        String scenario,

        String duration
) {
}
