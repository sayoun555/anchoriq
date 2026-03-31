package com.anchoriq.api.application.analytics;

import com.anchoriq.api.dto.response.analytics.AnalyticsResponse;
import com.anchoriq.api.dto.response.analytics.DistributionResponse;
import com.anchoriq.api.dto.response.analytics.TrendPointResponse;
import com.anchoriq.core.domain.analytics.service.RouteAnalyticsService;
import org.springframework.stereotype.Service;

import java.util.List;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RouteAnalyticsApplicationServiceImpl implements RouteAnalyticsApplicationService {

    private final RouteAnalyticsService routeAnalyticsService;

    @Override
    public AnalyticsResponse<List<DistributionResponse>> getHighRiskRoutes(int limit) {
        List<DistributionResponse> data = routeAnalyticsService.getHighRiskRoutes(limit)
                .stream().map(DistributionResponse::from).toList();
        return AnalyticsResponse.snapshot("high_risk_routes", data);
    }

    @Override
    public AnalyticsResponse<List<DistributionResponse>> getChokepointTraffic() {
        List<DistributionResponse> data = routeAnalyticsService.getChokepointTraffic()
                .stream().map(DistributionResponse::from).toList();
        return AnalyticsResponse.snapshot("chokepoint_traffic", data);
    }

    @Override
    public AnalyticsResponse<List<TrendPointResponse>> getChokepointRiskTrend(String name, int days) {
        List<TrendPointResponse> data = routeAnalyticsService.getChokepointRiskTrend(name, days)
                .stream().map(TrendPointResponse::from).toList();
        return AnalyticsResponse.daily("chokepoint_risk_trend", data);
    }
}
