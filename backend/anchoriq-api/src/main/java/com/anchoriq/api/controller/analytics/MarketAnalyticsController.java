package com.anchoriq.api.controller.analytics;

import com.anchoriq.api.application.analytics.MarketAnalyticsApplicationService;
import com.anchoriq.api.dto.response.analytics.AnalyticsResponse;
import com.anchoriq.api.dto.response.analytics.MarketCorrelationResponse;
import com.anchoriq.api.dto.response.analytics.TrendPointResponse;
import com.anchoriq.api.global.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * 시장 데이터 분석 API 컨트롤러.
 * 유가 추세, 환율 추세, 상관관계 분석을 제공한다.
 */
@RestController
@RequestMapping("/api/analytics/market")
@RequiredArgsConstructor
public class MarketAnalyticsController {

    private final MarketAnalyticsApplicationService applicationService;

    @GetMapping("/oil-price")
    public ApiResponse<AnalyticsResponse<List<TrendPointResponse>>> getOilPriceTrend(
            @RequestParam(defaultValue = "wti") String type,
            @RequestParam(defaultValue = "90") int days) {
        return ApiResponse.success(applicationService.getOilPriceTrend(type, days));
    }

    @GetMapping("/exchange-rate")
    public ApiResponse<AnalyticsResponse<List<TrendPointResponse>>> getExchangeRateTrend(
            @RequestParam(defaultValue = "USD-KRW") String pair,
            @RequestParam(defaultValue = "90") int days) {
        return ApiResponse.success(applicationService.getExchangeRateTrend(pair, days));
    }

    @GetMapping("/correlation")
    public ApiResponse<AnalyticsResponse<MarketCorrelationResponse>> getCorrelation(
            @RequestParam(defaultValue = "90") int days) {
        return ApiResponse.success(applicationService.getCorrelation(days));
    }
}
