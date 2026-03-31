package com.anchoriq.api.application.analytics;

import com.anchoriq.api.dto.response.analytics.AiUsageResponse;
import com.anchoriq.api.dto.response.analytics.AnalyticsResponse;
import com.anchoriq.api.dto.response.analytics.DistributionResponse;
import com.anchoriq.api.dto.response.analytics.TrendPointResponse;

import java.util.List;

public interface AiAnalyticsApplicationService {
    AnalyticsResponse<AiUsageResponse> getUsage();
    AnalyticsResponse<List<DistributionResponse>> getPopularQueries(int limit);
    AnalyticsResponse<List<TrendPointResponse>> getUsageTrend(int days);
}
