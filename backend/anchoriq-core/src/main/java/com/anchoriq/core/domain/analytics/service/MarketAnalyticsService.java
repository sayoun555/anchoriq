package com.anchoriq.core.domain.analytics.service;

import com.anchoriq.core.domain.analytics.model.TrendPoint;

import java.util.List;

/**
 * 시장 데이터 분석 서비스 인터페이스.
 * 유가 추세, 환율 추세, 상관관계 분석 등을 제공한다.
 * 구현체는 anchoriq-api의 infrastructure에 위치한다.
 */
public interface MarketAnalyticsService {

    /**
     * 유가 추세를 조회한다 (WTI 또는 Brent).
     */
    List<TrendPoint> getOilPriceTrend(String type, int days);

    /**
     * 환율 추세를 조회한다.
     */
    List<TrendPoint> getExchangeRateTrend(String pair, int days);

    /**
     * 유가-환율 상관계수를 계산한다 (-1.0 ~ 1.0).
     */
    double calculateCorrelation(int days);

    /**
     * 시장 데이터를 기록한다 (유가).
     */
    void recordOilPrice(String type, double price);

    /**
     * 시장 데이터를 기록한다 (환율).
     */
    void recordExchangeRate(String pair, double rate);
}
