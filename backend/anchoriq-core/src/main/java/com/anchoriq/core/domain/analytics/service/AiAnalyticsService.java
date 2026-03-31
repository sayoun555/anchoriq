package com.anchoriq.core.domain.analytics.service;

import com.anchoriq.core.domain.analytics.model.Distribution;
import com.anchoriq.core.domain.analytics.model.TrendPoint;

import java.util.List;

/**
 * AI 사용 통계 서비스 인터페이스.
 * 총 질의 수, 평균 응답 시간, 인기 질문, 사용 추세 등을 제공한다.
 * 구현체는 anchoriq-api의 infrastructure에 위치한다.
 */
public interface AiAnalyticsService {

    /**
     * 총 질의 수를 조회한다.
     */
    long getTotalQueries();

    /**
     * 오늘 질의 수를 조회한다.
     */
    long getTodayQueries();

    /**
     * 평균 응답 시간을 밀리초 단위로 조회한다.
     */
    double getAverageResponseTimeMs();

    /**
     * 인기 질문 Top N을 조회한다.
     */
    List<Distribution> getPopularQueries(int limit);

    /**
     * 일별 사용 추세를 조회한다.
     */
    List<TrendPoint> getUsageTrend(int days);

    /**
     * AI 질의를 기록한다.
     */
    void recordQuery(String query, long responseTimeMs);
}
