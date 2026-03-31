package com.anchoriq.api.controller.analytics;

import com.anchoriq.api.application.analytics.RouteAnalyticsApplicationService;
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
 * 항로/초크포인트 분석 API 컨트롤러.
 * 고위험 항로, 초크포인트 트래픽, 리스크 추세를 제공한다.
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class RouteAnalyticsController {

    private final RouteAnalyticsApplicationService applicationService;

    @GetMapping("/routes/high-risk-top10")
    public ApiResponse<AnalyticsResponse<List<DistributionResponse>>> getHighRiskRoutes() {
        return ApiResponse.success(applicationService.getHighRiskRoutes(10));
    }

    @GetMapping("/chokepoints/traffic")
    public ApiResponse<AnalyticsResponse<List<DistributionResponse>>> getChokepointTraffic() {
        return ApiResponse.success(applicationService.getChokepointTraffic());
    }

    @GetMapping("/chokepoints/risk-trend")
    public ApiResponse<AnalyticsResponse<List<TrendPointResponse>>> getChokepointRiskTrend(
            @RequestParam String name,
            @RequestParam(defaultValue = "30") int days) {
        return ApiResponse.success(applicationService.getChokepointRiskTrend(name, days));
    }
}
