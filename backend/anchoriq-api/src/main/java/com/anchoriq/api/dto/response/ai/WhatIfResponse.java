package com.anchoriq.api.dto.response.ai;

import com.anchoriq.ai.whatif.WhatIfResult;

import java.util.List;
import java.util.Map;

public record WhatIfResponse(
        String id,
        String scenario,
        String duration,
        int affectedVessels,
        List<String> affectedRoutes,
        String estimatedDelay,
        String additionalCost,
        List<Map<String, Object>> alternativeRoutes,
        List<String> recommendations,
        String aiAnalysis
) {
    public static WhatIfResponse from(WhatIfResult result) {
        return new WhatIfResponse(
                result.getId(),
                result.getScenario(),
                result.getDuration(),
                result.getAffectedVessels(),
                result.getAffectedRoutes(),
                result.getEstimatedDelay(),
                result.getAdditionalCost(),
                result.getAlternativeRoutes(),
                result.getRecommendations(),
                result.getAiAnalysis()
        );
    }
}
