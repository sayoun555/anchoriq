package com.anchoriq.api.controller.analytics;

import com.anchoriq.api.annotation.RequiresPlan;
import com.anchoriq.api.application.analytics.CollectorStatisticsApplicationService;
import com.anchoriq.api.dto.response.analytics.AnalyticsResponse;
import com.anchoriq.api.dto.response.analytics.CollectorStatisticsResponse;
import com.anchoriq.api.global.response.ApiResponse;
import com.anchoriq.core.domain.account.subscription.model.Plan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * 수집기 통계 API 컨트롤러.
 * 수집기별 처리량, 에러율, 마지막 실행 시간 통계를 제공한다.
 * Kafka가 비활성화된 환경에서는 Bean이 생성되지 않는다.
 */
@RestController
@RequestMapping("/api/admin/collectors/statistics")
@ConditionalOnBean(CollectorStatisticsApplicationService.class)
@RequiredArgsConstructor
public class CollectorStatisticsController {

    private final CollectorStatisticsApplicationService applicationService;

    @GetMapping
    @RequiresPlan(Plan.ENTERPRISE)
    public ApiResponse<AnalyticsResponse<List<CollectorStatisticsResponse>>> getAllStatistics() {
        return ApiResponse.success(applicationService.getAllStatistics());
    }

    @GetMapping("/{name}")
    @RequiresPlan(Plan.ENTERPRISE)
    public ApiResponse<AnalyticsResponse<CollectorStatisticsResponse>> getStatistics(
            @PathVariable String name) {
        return ApiResponse.success(applicationService.getStatistics(name));
    }
}
