package com.anchoriq.api.application.analytics;

import com.anchoriq.api.dto.response.analytics.AnalyticsResponse;
import com.anchoriq.api.dto.response.analytics.CollectorStatisticsResponse;

import java.util.List;

public interface CollectorStatisticsApplicationService {
    AnalyticsResponse<List<CollectorStatisticsResponse>> getAllStatistics();
    AnalyticsResponse<CollectorStatisticsResponse> getStatistics(String name);
}
