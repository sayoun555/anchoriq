package com.anchoriq.api.application.analytics;

import com.anchoriq.api.dto.response.analytics.AnalyticsResponse;
import com.anchoriq.api.dto.response.analytics.DistributionResponse;
import com.anchoriq.api.dto.response.analytics.TrendPointResponse;

import java.util.List;

public interface RouteAnalyticsApplicationService {
    AnalyticsResponse<List<DistributionResponse>> getHighRiskRoutes(int limit);
    AnalyticsResponse<List<DistributionResponse>> getChokepointTraffic();
    AnalyticsResponse<List<TrendPointResponse>> getChokepointRiskTrend(String name, int days);
}
