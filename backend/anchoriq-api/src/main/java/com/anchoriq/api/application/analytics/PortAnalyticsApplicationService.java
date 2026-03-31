package com.anchoriq.api.application.analytics;

import com.anchoriq.api.dto.response.analytics.AnalyticsResponse;
import com.anchoriq.api.dto.response.analytics.DistributionResponse;
import com.anchoriq.api.dto.response.analytics.TrendPointResponse;

import java.util.List;

public interface PortAnalyticsApplicationService {
    AnalyticsResponse<List<DistributionResponse>> getCongestionRanking(int limit);
    AnalyticsResponse<List<TrendPointResponse>> getCongestionTrend(String locode, int days);
    AnalyticsResponse<List<DistributionResponse>> getDistributionByRegion();
    AnalyticsResponse<List<DistributionResponse>> getAverageWaitTimeRanking();
}
