package com.anchoriq.api.application.analytics;

import com.anchoriq.api.dto.response.analytics.AiUsageResponse;
import com.anchoriq.api.dto.response.analytics.AnalyticsResponse;
import com.anchoriq.api.dto.response.analytics.DistributionResponse;
import com.anchoriq.api.dto.response.analytics.TrendPointResponse;
import com.anchoriq.core.domain.analytics.service.AiAnalyticsService;
import org.springframework.stereotype.Service;

import java.util.List;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiAnalyticsApplicationServiceImpl implements AiAnalyticsApplicationService {

    private final AiAnalyticsService aiAnalyticsService;

    @Override
    public AnalyticsResponse<AiUsageResponse> getUsage() {
        AiUsageResponse usage = new AiUsageResponse(
                aiAnalyticsService.getTotalQueries(),
                aiAnalyticsService.getTodayQueries(),
                aiAnalyticsService.getAverageResponseTimeMs()
        );
        return AnalyticsResponse.daily("ai_usage", usage);
    }

    @Override
    public AnalyticsResponse<List<DistributionResponse>> getPopularQueries(int limit) {
        List<DistributionResponse> data = aiAnalyticsService.getPopularQueries(limit)
                .stream().map(DistributionResponse::from).toList();
        return AnalyticsResponse.snapshot("ai_popular_queries", data);
    }

    @Override
    public AnalyticsResponse<List<TrendPointResponse>> getUsageTrend(int days) {
        List<TrendPointResponse> data = aiAnalyticsService.getUsageTrend(days)
                .stream().map(TrendPointResponse::from).toList();
        return AnalyticsResponse.daily("ai_usage_trend", data);
    }
}
