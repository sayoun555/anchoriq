package com.anchoriq.api.application.analytics;

import com.anchoriq.api.dto.response.analytics.AnalyticsResponse;
import com.anchoriq.api.dto.response.analytics.DistributionResponse;
import com.anchoriq.api.dto.response.analytics.TrendPointResponse;
import com.anchoriq.core.domain.analytics.service.PortAnalyticsService;
import org.springframework.stereotype.Service;

import java.util.List;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PortAnalyticsApplicationServiceImpl implements PortAnalyticsApplicationService {

    private final PortAnalyticsService portAnalyticsService;

    @Override
    public AnalyticsResponse<List<DistributionResponse>> getCongestionRanking(int limit) {
        List<DistributionResponse> data = portAnalyticsService.getCongestionRanking(limit)
                .stream().map(DistributionResponse::from).toList();
        return AnalyticsResponse.snapshot("port_congestion_ranking", data);
    }

    @Override
    public AnalyticsResponse<List<TrendPointResponse>> getCongestionTrend(String locode, int days) {
        List<TrendPointResponse> data = portAnalyticsService.getCongestionTrend(locode, days)
                .stream().map(TrendPointResponse::from).toList();
        return AnalyticsResponse.daily("port_congestion_trend", data);
    }

    @Override
    public AnalyticsResponse<List<DistributionResponse>> getDistributionByRegion() {
        List<DistributionResponse> data = portAnalyticsService.getDistributionByRegion()
                .stream().map(DistributionResponse::from).toList();
        return AnalyticsResponse.snapshot("port_by_region", data);
    }

    @Override
    public AnalyticsResponse<List<DistributionResponse>> getAverageWaitTimeRanking() {
        List<DistributionResponse> data = portAnalyticsService.getAverageWaitTimeRanking()
                .stream().map(DistributionResponse::from).toList();
        return AnalyticsResponse.snapshot("port_average_wait_time", data);
    }
}
