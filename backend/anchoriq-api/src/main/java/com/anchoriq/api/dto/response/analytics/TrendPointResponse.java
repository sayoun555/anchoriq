package com.anchoriq.api.dto.response.analytics;

import com.anchoriq.core.domain.analytics.model.TrendPoint;

/**
 * TrendPoint VO를 API 응답으로 변환하는 DTO.
 */
public record TrendPointResponse(
        String date,
        double value
) {

    public static TrendPointResponse from(TrendPoint trendPoint) {
        return new TrendPointResponse(
                trendPoint.date().toString(),
                trendPoint.value()
        );
    }
}
