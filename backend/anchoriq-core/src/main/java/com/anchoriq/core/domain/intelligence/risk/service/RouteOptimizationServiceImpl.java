package com.anchoriq.core.domain.intelligence.risk.service;

import com.anchoriq.core.domain.maritime.route.model.Route;
import com.anchoriq.core.domain.maritime.route.repository.RouteRepository;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 항로 최적화 도메인 서비스 구현체.
 * Bean 등록은 DomainServiceConfig에서 수행한다.
 */
public class RouteOptimizationServiceImpl implements RouteOptimizationService {

    private final RouteRepository routeRepository;

    public RouteOptimizationServiceImpl(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    @Override
    public List<Route> findAlternativeRoutes(String chokepointName) {
        return routeRepository.findAll().stream()
                .filter(route -> !route.passesThrough(chokepointName))
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> analyzeRouteCost(Route route) {
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("routeName", route.getName());
        analysis.put("distanceNm", route.getDistanceNm());
        analysis.put("estimatedDays", estimateTravelDays(route.getDistanceNm()));
        analysis.put("highRiskChokepoints", route.countHighRiskChokepoints());
        analysis.put("isHighRisk", route.isHighRisk());
        return analysis;
    }

    @Override
    public Route recommendSafestRoute(List<Route> candidates) {
        return candidates.stream()
                .min(Comparator.comparingInt(Route::countHighRiskChokepoints)
                        .thenComparingInt(Route::getDistanceNm))
                .orElse(null);
    }

    private int estimateTravelDays(int distanceNm) {
        int averageSpeedKnots = 14;
        return (int) Math.ceil((double) distanceNm / (averageSpeedKnots * 24));
    }
}
