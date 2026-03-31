package com.anchoriq.api.controller.analytics;

import com.anchoriq.api.application.analytics.AiAnalyticsApplicationService;
import com.anchoriq.api.dto.response.analytics.AiUsageResponse;
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
 * AI 사용 통계 API 컨트롤러.
 * 총 질의 수, 인기 질문, 사용 추세를 제공한다.
 */
@RestController
@RequestMapping("/api/analytics/ai")
@RequiredArgsConstructor
public class AiAnalyticsController {

    private final AiAnalyticsApplicationService applicationService;

    @GetMapping("/usage")
    public ApiResponse<AnalyticsResponse<AiUsageResponse>> getUsage() {
        return ApiResponse.success(applicationService.getUsage());
    }

    @GetMapping("/popular-queries")
    public ApiResponse<AnalyticsResponse<List<DistributionResponse>>> getPopularQueries(
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.success(applicationService.getPopularQueries(limit));
    }

    @GetMapping("/usage-trend")
    public ApiResponse<AnalyticsResponse<List<TrendPointResponse>>> getUsageTrend(
            @RequestParam(defaultValue = "30") int days) {
        return ApiResponse.success(applicationService.getUsageTrend(days));
    }
}
