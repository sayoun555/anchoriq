package com.anchoriq.api.application.risk;

import com.anchoriq.core.domain.intelligence.risk.model.RiskScore;

import java.util.Map;

/**
 * 리스크 스코어 Application Service 인터페이스.
 */
public interface RiskScoreApplicationService {

    RiskScore getVesselRiskScore(String vesselImo);

    RiskScore getRouteRiskScore(String routeId);

    RiskScore getPortRiskScore(String locode);

    RiskScore getChokepointRiskScore(String chokepointName);

    Map<String, Object> getDashboardSummary();

    Map<String, Object> getRiskTrends();
}
