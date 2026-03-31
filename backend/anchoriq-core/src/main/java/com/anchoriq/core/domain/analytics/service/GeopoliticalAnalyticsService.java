package com.anchoriq.core.domain.analytics.service;

import com.anchoriq.core.domain.analytics.model.Distribution;
import com.anchoriq.core.domain.analytics.model.TrendPoint;

import java.util.List;

/**
 * 지정학 리스크 분석 서비스 인터페이스.
 * 지역별 이벤트 빈도, 심각도 추세, 핫스팟 지역 등을 제공한다.
 * 구현체는 anchoriq-api의 infrastructure에 위치한다.
 */
public interface GeopoliticalAnalyticsService {

    /**
     * 지역별 지정학 이벤트 빈도를 조회한다.
     */
    List<Distribution> getEventsByRegion();

    /**
     * 심각도 추세를 조회한다.
     */
    List<TrendPoint> getSeverityTrend(int days);

    /**
     * 현재 핫스팟 지역 목록을 조회한다.
     */
    List<Distribution> getHotspots();

    /**
     * 지정학 이벤트를 기록한다.
     */
    void recordEvent(String region, double severity);
}
