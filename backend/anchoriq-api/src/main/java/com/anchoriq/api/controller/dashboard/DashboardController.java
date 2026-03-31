package com.anchoriq.api.controller.dashboard;

import com.anchoriq.api.application.dashboard.DashboardApplicationService;
import com.anchoriq.api.dto.response.dashboard.DashboardSummaryResponse;
import com.anchoriq.api.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 대시보드 전용 Controller.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardApplicationService dashboardApplicationService;

    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryResponse> getSummary() {
        return ApiResponse.success(dashboardApplicationService.getSummary());
    }

    @GetMapping("/recent-events")
    public ApiResponse<List<Map<String, Object>>> getRecentEvents(
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.success(dashboardApplicationService.getRecentEvents(limit));
    }

    @GetMapping("/chokepoint-status")
    public ApiResponse<List<Map<String, Object>>> getChokepointStatus() {
        return ApiResponse.success(dashboardApplicationService.getChokepointStatus());
    }

    @GetMapping("/risk-trend")
    public ApiResponse<List<Map<String, Object>>> getRiskTrend() {
        return ApiResponse.success(dashboardApplicationService.getRiskTrend());
    }

    @GetMapping("/congestion-ranking")
    public ApiResponse<List<Map<String, Object>>> getCongestionRanking() {
        return ApiResponse.success(dashboardApplicationService.getCongestionRanking());
    }

    @GetMapping("/top-risks")
    public ApiResponse<List<Map<String, Object>>> getTopRisks() {
        return ApiResponse.success(dashboardApplicationService.getTopRisks());
    }

    @GetMapping("/vessel-count-by-flag")
    public ApiResponse<Map<String, Long>> getVesselCountByFlag() {
        return ApiResponse.success(dashboardApplicationService.getVesselCountByFlag());
    }

    @GetMapping("/vessel-count-by-type")
    public ApiResponse<Map<String, Long>> getVesselCountByType() {
        return ApiResponse.success(dashboardApplicationService.getVesselCountByType());
    }
}
