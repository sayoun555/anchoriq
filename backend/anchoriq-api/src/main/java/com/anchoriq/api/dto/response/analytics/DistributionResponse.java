package com.anchoriq.api.dto.response.analytics;

import com.anchoriq.core.domain.analytics.model.Distribution;

/**
 * Distribution VO를 API 응답으로 변환하는 DTO.
 */
public record DistributionResponse(
        String label,
        long count,
        double percentage
) {

    public static DistributionResponse from(Distribution distribution) {
        return new DistributionResponse(
                distribution.label(),
                distribution.count(),
                distribution.percentage()
        );
    }
}
