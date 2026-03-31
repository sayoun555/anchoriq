package com.anchoriq.api.dto.response.analytics;

import java.time.Instant;

/**
 * 분석/통계 API 공통 응답 래퍼 DTO.
 * 메트릭 이름, 데이터, 계산 시점, 기간 정보를 포함한다.
 */
public record AnalyticsResponse<T>(
        String metric,
        T data,
        String calculatedAt,
        String period
) {

    public static <T> AnalyticsResponse<T> of(String metric, T data, String period) {
        return new AnalyticsResponse<>(metric, data, Instant.now().toString(), period);
    }

    public static <T> AnalyticsResponse<T> daily(String metric, T data) {
        return of(metric, data, "daily");
    }

    public static <T> AnalyticsResponse<T> snapshot(String metric, T data) {
        return of(metric, data, "snapshot");
    }
}
