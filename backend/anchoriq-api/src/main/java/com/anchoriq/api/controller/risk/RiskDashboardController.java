package com.anchoriq.api.controller.risk;

import com.anchoriq.api.application.dashboard.DashboardApplicationService;
import com.anchoriq.api.application.risk.RiskScoreApplicationService;
import com.anchoriq.api.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 리스크 대시보드 + 추세 + 타임라인 + 리포트 Controller.
 */
@RestController
@RequestMapping("/api/risk")
@RequiredArgsConstructor
public class RiskDashboardController {

    private final RiskScoreApplicationService riskScoreApplicationService;
    private final DashboardApplicationService dashboardApplicationService;

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> getDashboard() {
        return ApiResponse.success(riskScoreApplicationService.getDashboardSummary());
    }

    @GetMapping("/trends")
    public ApiResponse<Map<String, Object>> getTrends() {
        return ApiResponse.success(riskScoreApplicationService.getRiskTrends());
    }

    @GetMapping("/report/daily")
    public ApiResponse<Map<String, Object>> getDailyReport() {
        Map<String, Object> report = Map.of("message", "Daily risk report");
        return ApiResponse.success(report);
    }

    @GetMapping("/report/weekly")
    public ApiResponse<Map<String, Object>> getWeeklyReport() {
        Map<String, Object> report = Map.of("message", "Weekly risk report");
        return ApiResponse.success(report);
    }

    @GetMapping("/heatmap")
    public ApiResponse<List<Map<String, Object>>> getHeatmap() {
        try {
            return ApiResponse.success(dashboardApplicationService.getTopRisks());
        } catch (Exception e) {
            return ApiResponse.success(Collections.emptyList());
        }
    }

    @GetMapping("/alerts")
    public ApiResponse<Map<String, Object>> getAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(dashboardApplicationService.getRiskAlerts(page, size));
    }
}
