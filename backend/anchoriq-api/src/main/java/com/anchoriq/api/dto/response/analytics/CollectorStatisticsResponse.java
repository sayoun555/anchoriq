package com.anchoriq.api.dto.response.analytics;

import com.anchoriq.core.domain.analytics.model.CollectorStatistics;

/**
 * CollectorStatistics VO를 API 응답으로 변환하는 DTO.
 */
public record CollectorStatisticsResponse(
        String name,
        long todayProcessed,
        long todayErrors,
        double errorRate,
        String lastSuccessAt,
        String lastErrorAt,
        long totalProcessed,
        boolean healthy,
        boolean recentError
) {

    public static CollectorStatisticsResponse from(CollectorStatistics stats) {
        return new CollectorStatisticsResponse(
                stats.name().value(),
                stats.todayProcessed(),
                stats.todayErrors(),
                stats.errorRate(),
                stats.lastSuccessAt() != null ? stats.lastSuccessAt().toString() : null,
                stats.lastErrorAt() != null ? stats.lastErrorAt().toString() : null,
                stats.totalProcessed(),
                stats.isHealthy(),
                stats.hasRecentError()
        );
    }
}
