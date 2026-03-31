package com.anchoriq.api.application.analytics;

import com.anchoriq.api.dto.response.analytics.AnalyticsResponse;
import com.anchoriq.api.dto.response.analytics.DistributionResponse;

import java.util.List;

/**
 * 선박 분석 Application Service 인터페이스.
 */
public interface VesselAnalyticsApplicationService {

    AnalyticsResponse<List<DistributionResponse>> getDistributionByFlag();

    AnalyticsResponse<List<DistributionResponse>> getDistributionByType();

    AnalyticsResponse<List<DistributionResponse>> getDistributionByAgeRange();

    AnalyticsResponse<Double> getSanctionedRatio();

    AnalyticsResponse<List<DistributionResponse>> getRiskScoreDistribution();
}
