package com.anchoriq.api.application.dashboard;

import com.anchoriq.api.dto.response.dashboard.DashboardSummaryResponse;

import java.util.List;
import java.util.Map;

/**
 * 대시보드 Application Service 인터페이스.
 */
public interface DashboardApplicationService {

    DashboardSummaryResponse getSummary();

    List<Map<String, Object>> getRecentEvents(int limit);

    List<Map<String, Object>> getChokepointStatus();

    List<Map<String, Object>> getRiskTrend();

    List<Map<String, Object>> getCongestionRanking();

    Map<String, Object> getRiskAlerts(int page, int size);

    List<Map<String, Object>> getTopRisks();

    Map<String, Long> getVesselCountByFlag();

    Map<String, Long> getVesselCountByType();
}
