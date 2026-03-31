package com.anchoriq.api.dto.response.dashboard;

import lombok.Builder;
import lombok.Getter;

/**
 * 대시보드 요약 응답 DTO.
 */
@Getter
@Builder
public class DashboardSummaryResponse {

    private final long totalVessels;
    private final long totalPorts;
    private final long totalAlerts;
    private final long highRiskCount;
    private final long mediumRiskCount;
    private final long lowRiskCount;
}
