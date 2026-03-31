package com.anchoriq.core.domain.analytics.service;

import com.anchoriq.core.domain.analytics.model.Distribution;
import com.anchoriq.core.domain.analytics.model.TrendPoint;

import java.util.List;

/**
 * 항만 분석 서비스 인터페이스.
 * 혼잡도 랭킹, 추세, 지역별 분포, 평균 대기 시간 등을 제공한다.
 * 구현체는 anchoriq-api의 infrastructure에 위치한다.
 */
public interface PortAnalyticsService {

    /**
     * 혼잡도 상위 랭킹을 조회한다.
     */
    List<Distribution> getCongestionRanking(int limit);

    /**
     * 특정 항만의 혼잡도 추세를 조회한다.
     */
    List<TrendPoint> getCongestionTrend(String locode, int days);

    /**
     * 지역별 항만 수 및 평균 혼잡도를 조회한다.
     */
    List<Distribution> getDistributionByRegion();

    /**
     * 평균 대기 시간 랭킹을 조회한다.
     */
    List<Distribution> getAverageWaitTimeRanking();
}
