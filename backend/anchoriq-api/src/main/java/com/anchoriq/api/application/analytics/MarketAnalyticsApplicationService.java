package com.anchoriq.api.application.analytics;

import com.anchoriq.api.dto.response.analytics.AnalyticsResponse;
import com.anchoriq.api.dto.response.analytics.MarketCorrelationResponse;
import com.anchoriq.api.dto.response.analytics.TrendPointResponse;

import java.util.List;

public interface MarketAnalyticsApplicationService {
    AnalyticsResponse<List<TrendPointResponse>> getOilPriceTrend(String type, int days);
    AnalyticsResponse<List<TrendPointResponse>> getExchangeRateTrend(String pair, int days);
    AnalyticsResponse<MarketCorrelationResponse> getCorrelation(int days);
}
