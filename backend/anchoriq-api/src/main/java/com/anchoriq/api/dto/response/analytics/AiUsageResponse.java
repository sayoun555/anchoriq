package com.anchoriq.api.dto.response.analytics;

/**
 * AI 사용 통계 요약 응답 DTO.
 */
public record AiUsageResponse(
        long totalQueries,
        long todayQueries,
        double averageResponseTimeMs
) {
}
