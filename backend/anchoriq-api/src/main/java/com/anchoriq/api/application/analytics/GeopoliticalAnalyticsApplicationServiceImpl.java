package com.anchoriq.api.application.analytics;

import com.anchoriq.api.dto.response.analytics.AnalyticsResponse;
import com.anchoriq.api.dto.response.analytics.DistributionResponse;
import com.anchoriq.api.dto.response.analytics.TrendPointResponse;
import com.anchoriq.core.domain.analytics.service.GeopoliticalAnalyticsService;
import org.springframework.stereotype.Service;

import java.util.List;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GeopoliticalAnalyticsApplicationServiceImpl implements GeopoliticalAnalyticsApplicationService {

    private final GeopoliticalAnalyticsService geopoliticalAnalyticsService;

    @Override
    public AnalyticsResponse<List<DistributionResponse>> getEventsByRegion() {
        List<DistributionResponse> data = geopoliticalAnalyticsService.getEventsByRegion()
                .stream().map(DistributionResponse::from).toList();
        return AnalyticsResponse.snapshot("geopolitical_by_region", data);
    }

    @Override
    public AnalyticsResponse<List<TrendPointResponse>> getSeverityTrend(int days) {
        List<TrendPointResponse> data = geopoliticalAnalyticsService.getSeverityTrend(days)
                .stream().map(TrendPointResponse::from).toList();
        return AnalyticsResponse.daily("geopolitical_severity_trend", data);
    }

    @Override
    public AnalyticsResponse<List<DistributionResponse>> getHotspots() {
        List<DistributionResponse> data = geopoliticalAnalyticsService.getHotspots()
                .stream().map(DistributionResponse::from).toList();
        return AnalyticsResponse.snapshot("geopolitical_hotspots", data);
    }
}
