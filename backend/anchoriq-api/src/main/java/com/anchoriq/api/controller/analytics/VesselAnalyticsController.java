package com.anchoriq.api.controller.analytics;

import com.anchoriq.api.application.analytics.VesselAnalyticsApplicationService;
import com.anchoriq.api.dto.response.analytics.AnalyticsResponse;
import com.anchoriq.api.dto.response.analytics.DistributionResponse;
import com.anchoriq.api.global.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * 선박 분석 API 컨트롤러.
 * 국적별, 타입별, 선령별, 리스크별 선박 분포를 제공한다.
 */
@RestController
@RequestMapping("/api/analytics/vessels")
@RequiredArgsConstructor
public class VesselAnalyticsController {

    private final VesselAnalyticsApplicationService applicationService;

    @GetMapping("/by-flag")
    public ApiResponse<AnalyticsResponse<List<DistributionResponse>>> getByFlag() {
        return ApiResponse.success(applicationService.getDistributionByFlag());
    }

    @GetMapping("/by-type")
    public ApiResponse<AnalyticsResponse<List<DistributionResponse>>> getByType() {
        return ApiResponse.success(applicationService.getDistributionByType());
    }

    @GetMapping("/by-age")
    public ApiResponse<AnalyticsResponse<List<DistributionResponse>>> getByAge() {
        return ApiResponse.success(applicationService.getDistributionByAgeRange());
    }

    @GetMapping("/sanctioned-ratio")
    public ApiResponse<AnalyticsResponse<Double>> getSanctionedRatio() {
        return ApiResponse.success(applicationService.getSanctionedRatio());
    }

    @GetMapping("/risk-distribution")
    public ApiResponse<AnalyticsResponse<List<DistributionResponse>>> getRiskDistribution() {
        return ApiResponse.success(applicationService.getRiskScoreDistribution());
    }
}
