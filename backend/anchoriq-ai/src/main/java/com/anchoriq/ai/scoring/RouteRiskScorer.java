package com.anchoriq.ai.scoring;

import com.anchoriq.core.common.exception.EntityNotFoundException;
import com.anchoriq.core.domain.intelligence.risk.model.RiskScore;
import com.anchoriq.core.domain.intelligence.risk.service.SupplyChainRiskService;
import com.anchoriq.core.domain.maritime.route.model.Route;
import com.anchoriq.core.domain.maritime.route.repository.RouteRepository;
import org.springframework.stereotype.Component;

/**
 * 항로 리스크 스코어러.
 */
@Component
public class RouteRiskScorer implements RiskScorer {

    private final RouteRepository routeRepository;
    private final SupplyChainRiskService riskService;

    public RouteRiskScorer(RouteRepository routeRepository,
                           SupplyChainRiskService riskService) {
        this.routeRepository = routeRepository;
        this.riskService = riskService;
    }

    @Override
    public RiskScore calculateScore(String routeId) {
        Route route = routeRepository.findById(Long.parseLong(routeId))
                .orElseThrow(() -> new EntityNotFoundException("Route", routeId));
        return riskService.assessRouteRisk(route);
    }

    @Override
    public String getTargetType() {
        return "ROUTE";
    }
}
