package com.anchoriq.api.application.analytics;

import com.anchoriq.api.dto.response.analytics.AnalyticsResponse;
import com.anchoriq.api.dto.response.analytics.CollectorStatisticsResponse;
import com.anchoriq.core.domain.analytics.service.CollectorStatisticsService;
import com.anchoriq.core.domain.operation.collector.model.CollectorName;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.List;
import lombok.RequiredArgsConstructor;

@Service
@ConditionalOnBean(CollectorStatisticsService.class)
@RequiredArgsConstructor
public class CollectorStatisticsApplicationServiceImpl implements CollectorStatisticsApplicationService {

    private final CollectorStatisticsService collectorStatisticsService;

    @Override
    public AnalyticsResponse<List<CollectorStatisticsResponse>> getAllStatistics() {
        List<CollectorStatisticsResponse> stats = collectorStatisticsService.getAllStatistics()
                .stream()
                .map(CollectorStatisticsResponse::from)
                .toList();
        return AnalyticsResponse.daily("collector_statistics", stats);
    }

    @Override
    public AnalyticsResponse<CollectorStatisticsResponse> getStatistics(String name) {
        CollectorName collectorName = CollectorName.from(name);
        CollectorStatisticsResponse response = CollectorStatisticsResponse.from(
                collectorStatisticsService.getStatistics(collectorName));
        return AnalyticsResponse.daily("collector_statistics_detail", response);
    }
}
