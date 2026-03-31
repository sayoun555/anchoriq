package com.anchoriq.core.domain.analytics.service;

import com.anchoriq.core.domain.analytics.model.Distribution;

import java.util.List;

/**
 * 선박 분석 서비스 인터페이스.
 * 국적별, 타입별, 선령별, 리스크별 선박 분포를 제공한다.
 * 구현체는 anchoriq-api의 infrastructure에 위치한다.
 */
public interface VesselAnalyticsService {

    /**
     * 국적(Flag)별 선박 분포를 조회한다.
     */
    List<Distribution> getDistributionByFlag();

    /**
     * 선박 타입별 분포를 조회한다 (탱커, 벌크, 컨테이너 등).
     */
    List<Distribution> getDistributionByType();

    /**
     * 선령별 분포를 조회한다 (0-5, 5-10, 10-15, 15-20, 20+).
     */
    List<Distribution> getDistributionByAgeRange();

    /**
     * 제재국 선박 비율을 조회한다.
     */
    double getSanctionedRatio();

    /**
     * 리스크 점수 분포를 조회한다 (0-20, 20-40, 40-60, 60-80, 80-100).
     */
    List<Distribution> getRiskScoreDistribution();
}
