package com.anchoriq.core.domain.intelligence.risk.service;

import com.anchoriq.core.domain.intelligence.risk.model.RiskAssessment;
import com.anchoriq.core.domain.intelligence.risk.model.RiskScore;
import com.anchoriq.core.domain.maritime.port.model.Port;
import com.anchoriq.core.domain.maritime.route.model.Chokepoint;
import com.anchoriq.core.domain.maritime.route.model.Route;
import com.anchoriq.core.domain.maritime.vessel.model.Vessel;

/**
 * 공급망 리스크 도메인 서비스.
 * 여러 Aggregate(Vessel, Route, Weather, Sanction)를 조합하여 종합 리스크를 판단한다.
 */
public interface SupplyChainRiskService {

    RiskScore assessVesselRisk(Vessel vessel);

    RiskScore assessRouteRisk(Route route);

    RiskScore assessPortRisk(Port port);

    RiskScore assessChokepointRisk(Chokepoint chokepoint);

    RiskAssessment createFullAssessment(String targetId, String targetType);
}
