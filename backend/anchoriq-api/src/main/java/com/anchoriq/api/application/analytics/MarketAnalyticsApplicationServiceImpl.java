package com.anchoriq.api.application.analytics;

import com.anchoriq.api.dto.response.analytics.AnalyticsResponse;
import com.anchoriq.api.dto.response.analytics.MarketCorrelationResponse;
import com.anchoriq.api.dto.response.analytics.TrendPointResponse;
import com.anchoriq.core.domain.analytics.service.MarketAnalyticsService;
import org.springframework.stereotype.Service;

import java.util.List;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MarketAnalyticsApplicationServiceImpl implements MarketAnalyticsApplicationService {

    private final MarketAnalyticsService marketAnalyticsService;

    @Override
    public AnalyticsResponse<List<TrendPointResponse>> getOilPriceTrend(String type, int days) {
        List<TrendPointResponse> data = marketAnalyticsService.getOilPriceTrend(type, days)
                .stream().map(TrendPointResponse::from).toList();
        return AnalyticsResponse.daily("oil_price_trend", data);
    }

    @Override
    public AnalyticsResponse<List<TrendPointResponse>> getExchangeRateTrend(String pair, int days) {
        List<TrendPointResponse> data = marketAnalyticsService.getExchangeRateTrend(pair, days)
                .stream().map(TrendPointResponse::from).toList();
        return AnalyticsResponse.daily("exchange_rate_trend", data);
    }

    @Override
    public AnalyticsResponse<MarketCorrelationResponse> getCorrelation(int days) {
        double correlation = marketAnalyticsService.calculateCorrelation(days);
        MarketCorrelationResponse response = MarketCorrelationResponse.of(correlation);
        return AnalyticsResponse.snapshot("oil_exchange_correlation", response);
    }
}
