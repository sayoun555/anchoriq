package com.anchoriq.api.controller.analytics;

import com.anchoriq.api.application.analytics.GeopoliticalAnalyticsApplicationService;
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
 * 지정학 리스크 분석 API 컨트롤러.
 * 지역별 이벤트 빈도, 심각도 추세, 핫스팟 지역을 제공한다.
 */
@RestController
@RequestMapping("/api/analytics/geopolitical")
@RequiredArgsConstructor
public class GeopoliticalAnalyticsController {

    private final GeopoliticalAnalyticsApplicationService applicationService;

    @GetMapping("/by-region")
    public ApiResponse<AnalyticsResponse<List<DistributionResponse>>> getByRegion() {
        return ApiResponse.success(applicationService.getEventsByRegion());
    }

    @GetMapping("/severity-trend")
    public ApiResponse<AnalyticsResponse<List<TrendPointResponse>>> getSeverityTrend(
            @RequestParam(defaultValue = "30") int days) {
        return ApiResponse.success(applicationService.getSeverityTrend(days));
    }

    @GetMapping("/hotspots")
    public ApiResponse<AnalyticsResponse<List<DistributionResponse>>> getHotspots() {
        return ApiResponse.success(applicationService.getHotspots());
    }
}
