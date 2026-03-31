package com.anchoriq.api.dto.response.payment;

import java.math.BigDecimal;
import java.util.List;

public record PlanResponse(
        String name,
        BigDecimal priceUsd,
        BigDecimal priceKrw,
        List<String> features,
        int dailyApiLimit
) {}
