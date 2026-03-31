package com.anchoriq.api.controller.analytics;

import com.anchoriq.api.application.analytics.PortAnalyticsApplicationService;
import com.anchoriq.api.dto.response.analytics.AnalyticsResponse;
import com.anchoriq.api.dto.response.analytics.DistributionResponse;
import com.anchoriq.api.dto.response.analytics.TrendPointResponse;
import com.anchoriq.api.global.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * 항만 분석 API 컨트롤러.
 * 혼잡도 랭킹, 추세, 지역별 분포, 평균 대기 시간을 제공한다.
 */
@RestController
@RequestMapping("/api/analytics/ports")
@RequiredArgsConstructor
public class PortAnalyticsController {

    private final PortAnalyticsApplicationService applicationService;

    @GetMapping("/congestion-ranking")
    public ApiResponse<AnalyticsResponse<List<DistributionResponse>>> getCongestionRanking(
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(applicationService.getCongestionRanking(limit));
    }

    @GetMapping("/congestion-trend")
    public ApiResponse<AnalyticsResponse<List<TrendPointResponse>>> getCongestionTrend(
            @RequestParam String locode,
            @RequestParam(defaultValue = "30") int days) {
        return ApiResponse.success(applicationService.getCongestionTrend(locode, days));
    }

    @GetMapping("/by-region")
    public ApiResponse<AnalyticsResponse<List<DistributionResponse>>> getByRegion() {
        return ApiResponse.success(applicationService.getDistributionByRegion());
    }

    @GetMapping("/average-wait-time")
    public ApiResponse<AnalyticsResponse<List<DistributionResponse>>> getAverageWaitTime() {
        return ApiResponse.success(applicationService.getAverageWaitTimeRanking());
    }
}
