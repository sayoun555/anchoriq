package com.anchoriq.core.domain.analytics.service;

import com.anchoriq.core.domain.analytics.model.Distribution;
import com.anchoriq.core.domain.analytics.model.TrendPoint;

import java.util.List;

/**
 * 항로/초크포인트 분석 서비스 인터페이스.
 * 고위험 항로, 초크포인트 트래픽, 리스크 추세 등을 제공한다.
 * 구현체는 anchoriq-api의 infrastructure에 위치한다.
 */
public interface RouteAnalyticsService {

    /**
     * 고위험 항로 Top N을 조회한다.
     */
    List<Distribution> getHighRiskRoutes(int limit);

    /**
     * 초크포인트별 통과 선박 수를 조회한다.
     */
    List<Distribution> getChokepointTraffic();

    /**
     * 특정 초크포인트의 리스크 추세를 조회한다.
     */
    List<TrendPoint> getChokepointRiskTrend(String chokepointName, int days);
}
