package com.anchoriq.api.application.analytics;

import com.anchoriq.api.dto.response.analytics.AnalyticsResponse;
import com.anchoriq.api.dto.response.analytics.DistributionResponse;
import com.anchoriq.api.dto.response.analytics.TrendPointResponse;

import java.util.List;

public interface GeopoliticalAnalyticsApplicationService {
    AnalyticsResponse<List<DistributionResponse>> getEventsByRegion();
    AnalyticsResponse<List<TrendPointResponse>> getSeverityTrend(int days);
    AnalyticsResponse<List<DistributionResponse>> getHotspots();
}
